package com.afrodebab.cms.jpa.entity;

import java.util.List;
import java.util.Set;

/**
 * One manager-defined question on a job's application form, stored as JSON in
 * {@code jobs.application_fields}. {@code fileTypes} is used by FILE fields only,
 * {@code maxLength} by TEXT fields only.
 */
public record JobApplicationField(
        String id,
        Type type,
        String label,
        String helpText,
        boolean required,
        List<FileType> fileTypes,
        Integer maxLength
) {
    public enum Type { LINK, FILE, TEXT }

    /** Readable document formats only — no media or executables. */
    public enum FileType {
        PDF(Set.of("pdf")),
        DOC(Set.of("doc")),
        DOCX(Set.of("docx")),
        ODT(Set.of("odt")),
        RTF(Set.of("rtf")),
        TXT(Set.of("txt")),
        MD(Set.of("md", "markdown"));

        private final Set<String> extensions;

        FileType(Set<String> extensions) { this.extensions = extensions; }

        public Set<String> extensions() { return extensions; }
    }
}
