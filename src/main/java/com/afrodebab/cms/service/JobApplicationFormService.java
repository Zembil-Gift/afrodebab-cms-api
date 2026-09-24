package com.afrodebab.cms.service;

import com.afrodebab.cms.exception.BadRequestException;
import com.afrodebab.cms.jpa.entity.Job;
import com.afrodebab.cms.jpa.entity.JobApplicationAnswer;
import com.afrodebab.cms.jpa.entity.JobApplicationField;
import com.afrodebab.cms.jpa.entity.JobApplicationField.FileType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * The per-job application form: validates the fields a manager defines, and the answers
 * (links, files, written text) an applicant submits against them.
 *
 * <p>Multipart keys: {@code answer.<fieldId>} for LINK/TEXT answers, {@code file.<fieldId>} for files.
 */
@Service
public class JobApplicationFormService {

    static final int MAX_FIELDS = 15;
    static final int MAX_LABEL = 80;
    static final int MAX_HELP_TEXT = 300;
    static final int DEFAULT_TEXT_LENGTH = 2000;
    static final int MAX_TEXT_LENGTH = 5000;
    static final int MAX_LINK_LENGTH = 2000;
    static final long MAX_FILE_BYTES = 10L * 1024 * 1024;

    private static final Pattern FIELD_ID = Pattern.compile("^[a-z0-9-]{1,40}$");
    private static final Set<FileType> RESUME_TYPES = EnumSet.of(FileType.PDF, FileType.DOC, FileType.DOCX);

    private final CloudflareR2Service r2Service;

    public JobApplicationFormService(CloudflareR2Service r2Service) {
        this.r2Service = r2Service;
    }

    /** A validated answer not yet persisted; files are uploaded only once the application exists. */
    public record PendingAnswer(JobApplicationField field, String value, MultipartFile file) {}

    // ---- manager side: form definition ----

    public List<JobApplicationField> normalizeFields(List<JobApplicationField> fields) {
        if (fields == null) return new ArrayList<>();
        if (fields.size() > MAX_FIELDS) {
            throw new BadRequestException("An application form can have at most " + MAX_FIELDS + " fields");
        }
        Set<String> ids = new HashSet<>();
        List<JobApplicationField> out = new ArrayList<>();
        for (JobApplicationField f : fields) {
            JobApplicationField n = normalizeField(f);
            if (!ids.add(n.id())) throw new BadRequestException("Duplicate application field id: " + n.id());
            out.add(n);
        }
        return out;
    }

    private JobApplicationField normalizeField(JobApplicationField f) {
        if (f == null || f.type() == null) throw new BadRequestException("Each application field needs a type");
        String label = trimToNull(f.label());
        if (label == null) throw new BadRequestException("Each application field needs a title");
        if (label.length() > MAX_LABEL) throw new BadRequestException("Field title must be at most " + MAX_LABEL + " characters");
        String help = trimToNull(f.helpText());
        if (help != null && help.length() > MAX_HELP_TEXT) {
            throw new BadRequestException("Field description must be at most " + MAX_HELP_TEXT + " characters");
        }

        String id = trimToNull(f.id());
        if (id == null) id = "f-" + UUID.randomUUID().toString().substring(0, 8);
        if (!FIELD_ID.matcher(id).matches()) throw new BadRequestException("Invalid application field id: " + id);

        List<FileType> fileTypes = null;
        Integer maxLength = null;
        switch (f.type()) {
            case FILE -> {
                if (f.fileTypes() == null || f.fileTypes().isEmpty() || f.fileTypes().contains(null)) {
                    throw new BadRequestException("File field \"" + label + "\" needs at least one allowed file type");
                }
                fileTypes = List.copyOf(EnumSet.copyOf(f.fileTypes()));
            }
            case TEXT -> {
                maxLength = f.maxLength() == null ? DEFAULT_TEXT_LENGTH : f.maxLength();
                if (maxLength < 1 || maxLength > MAX_TEXT_LENGTH) {
                    throw new BadRequestException("Written answer limit must be between 1 and " + MAX_TEXT_LENGTH + " characters");
                }
            }
            case LINK -> { }
        }
        return new JobApplicationField(id, f.type(), label, help, f.required(), fileTypes, maxLength);
    }

    // ---- applicant side: answers ----

    public void checkResume(MultipartFile resume) {
        if (resume == null || resume.isEmpty()) throw new BadRequestException("Resume is required");
        checkFile("Resume", resume, RESUME_TYPES);
    }

    /** Validates every answer up front so nothing is stored or uploaded for a rejected application. */
    public List<PendingAnswer> validateAnswers(Job job, Map<String, String[]> params, Map<String, MultipartFile> files) {
        List<PendingAnswer> pending = new ArrayList<>();
        for (JobApplicationField field : job.getApplicationFields()) {
            if (field.type() == JobApplicationField.Type.FILE) {
                MultipartFile file = files == null ? null : files.get("file." + field.id());
                if (file == null || file.isEmpty()) {
                    if (field.required()) throw new BadRequestException("\"" + field.label() + "\" is required");
                    continue;
                }
                checkFile(field.label(), file, EnumSet.copyOf(field.fileTypes()));
                pending.add(new PendingAnswer(field, null, file));
            } else {
                String[] raw = params == null ? null : params.get("answer." + field.id());
                String value = raw == null || raw.length == 0 ? null : trimToNull(raw[0]);
                if (value == null) {
                    if (field.required()) throw new BadRequestException("\"" + field.label() + "\" is required");
                    continue;
                }
                if (field.type() == JobApplicationField.Type.LINK) checkLink(field.label(), value);
                else checkText(field, value);
                pending.add(new PendingAnswer(field, value, null));
            }
        }
        return pending;
    }

    public List<JobApplicationAnswer> store(Long applicationId, List<PendingAnswer> pending) {
        return pending.stream().map(p -> {
            JobApplicationField f = p.field();
            if (p.file() == null) return new JobApplicationAnswer(f.id(), f.label(), f.type(), p.value(), null);
            String url = r2Service.uploadJobApplicationAttachment(applicationId, p.file());
            return new JobApplicationAnswer(f.id(), f.label(), f.type(), url, p.file().getOriginalFilename());
        }).toList();
    }

    private void checkLink(String label, String value) {
        if (value.length() > MAX_LINK_LENGTH) throw new BadRequestException("\"" + label + "\" link is too long");
        try {
            URI uri = new URI(value);
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            String host = uri.getHost();
            if (!(scheme.equals("http") || scheme.equals("https")) || host == null || !host.contains(".")) {
                throw new BadRequestException("\"" + label + "\" must be a valid link starting with http:// or https://");
            }
        } catch (URISyntaxException e) {
            throw new BadRequestException("\"" + label + "\" must be a valid link starting with http:// or https://");
        }
    }

    private void checkText(JobApplicationField field, String value) {
        int limit = field.maxLength() == null ? DEFAULT_TEXT_LENGTH : field.maxLength();
        if (value.length() > limit) {
            throw new BadRequestException("\"" + field.label() + "\" must be at most " + limit + " characters");
        }
    }

    /** Checks size, extension and the file's actual leading bytes, so a renamed video can't pass as a PDF. */
    private void checkFile(String label, MultipartFile file, Set<FileType> allowed) {
        if (file.getSize() > MAX_FILE_BYTES) throw new BadRequestException("\"" + label + "\" must be 10 MB or smaller");

        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        int dot = name.lastIndexOf('.');
        String ext = dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
        FileType type = allowed.stream().filter(t -> t.extensions().contains(ext)).findFirst()
                .orElseThrow(() -> new BadRequestException("\"" + label + "\" must be one of: " + describe(allowed)));

        byte[] head = readHead(file);
        if (!matchesSignature(type, head)) {
            throw new BadRequestException("\"" + label + "\" does not look like a valid " + type.name() + " file");
        }
    }

    private static boolean matchesSignature(FileType type, byte[] head) {
        return switch (type) {
            case PDF -> startsWith(head, "%PDF-".getBytes());
            case DOCX, ODT -> startsWith(head, new byte[]{'P', 'K', 3, 4});
            case DOC -> startsWith(head, new byte[]{(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0, (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1});
            case RTF -> startsWith(head, "{\\rtf".getBytes());
            // ponytail: plain-text check is "no NUL byte in the first 8 KB" — catches binaries, not every oddity.
            case TXT, MD -> {
                for (byte b : head) if (b == 0) yield false;
                yield true;
            }
        };
    }

    private static byte[] readHead(MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            return in.readNBytes(8192);
        } catch (IOException e) {
            throw new BadRequestException("Could not read uploaded file");
        }
    }

    private static boolean startsWith(byte[] data, byte[] prefix) {
        return data.length >= prefix.length && Arrays.equals(Arrays.copyOf(data, prefix.length), prefix);
    }

    private static String describe(Set<FileType> types) {
        return String.join(", ", types.stream().map(Enum::name).toList());
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
