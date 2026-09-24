package com.afrodebab.cms.service;

import com.afrodebab.cms.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.net.URI;
import java.util.UUID;

@Service
public class CloudflareR2Service {
    private final String s3Api;
    private final String publicDevelopmentUrl;
    private final String region;
    private final String accessKeyId;
    private final String secretAccessKey;

    public CloudflareR2Service(@Value("${app.cloudflare.r2.s3Api:}") String s3Api,
                               @Value("${app.cloudflare.r2.publicDevelopmentUrl:}") String publicDevelopmentUrl,
                               @Value("${app.cloudflare.r2.accessKeyId:}") String accessKeyId,
                               @Value("${app.cloudflare.r2.secretAccessKey:}") String secretAccessKey,
                               @Value("${app.cloudflare.r2.region:auto}") String region) {
        this.s3Api = s3Api;
        this.publicDevelopmentUrl = publicDevelopmentUrl;
        this.region = region;
        this.accessKeyId = accessKeyId;
        this.secretAccessKey = secretAccessKey;
    }

    public String uploadEmployeePhoto(Long employeeId, MultipartFile file) {
        if (file == null || file.isEmpty()) throw new BadRequestException("Photo file is required");
        return uploadFile("employees/" + employeeId, file, "photo", "Failed to upload employee photo");
    }

    public String uploadOrgLogo(Long orgId, MultipartFile file) {
        if (file == null || file.isEmpty()) throw new BadRequestException("Logo file is required");
        return uploadFile("organizations/" + orgId + "/logo", file, "logo", "Failed to upload organization logo");
    }

    public String uploadOrgEmailLogo(Long orgId, MultipartFile file) {
        if (file == null || file.isEmpty()) throw new BadRequestException("Logo file is required");
        return uploadFile("organizations/" + orgId + "/email-logo", file, "email-logo", "Failed to upload email logo");
    }

    public String uploadOrgCover(Long orgId, MultipartFile file) {
        if (file == null || file.isEmpty()) throw new BadRequestException("Cover file is required");
        return uploadFile("organizations/" + orgId + "/cover", file, "cover", "Failed to upload organization cover");
    }

    /** Cover image for an org's blog post or event; {@code kind} is the folder, e.g. "blogs" or "events". */
    public String uploadOrgContentImage(Long orgId, String kind, MultipartFile file) {
        if (file == null || file.isEmpty()) throw new BadRequestException("Image file is required");
        String type = file.getContentType();
        if (type == null || !type.startsWith("image/")) throw new BadRequestException("Only image files are allowed");
        return uploadFile("organizations/" + orgId + "/" + kind, file, "image", "Failed to upload image");
    }

    public String uploadJobApplicationResume(Long applicationId, MultipartFile file) {
        if (file == null || file.isEmpty()) throw new BadRequestException("Resume file is required");
        return uploadFile("job-applications/" + applicationId, file, "resume", "Failed to upload application resume");
    }

    public String uploadJobApplicationAttachment(Long applicationId, MultipartFile file) {
        if (file == null || file.isEmpty()) throw new BadRequestException("Attachment file is required");
        return uploadFile("job-applications/" + applicationId + "/attachments", file, "attachment", "Failed to upload application attachment");
    }

    private String uploadFile(String folder, MultipartFile file, String fallbackName, String uploadError) {
        if (s3Api == null || s3Api.isBlank()) throw new IllegalStateException("S3_API is not configured");
        if (accessKeyId == null || accessKeyId.isBlank()) throw new IllegalStateException("ACCESS_KEY_ID is not configured");
        if (secretAccessKey == null || secretAccessKey.isBlank()) throw new IllegalStateException("SECRET_ACCESS_KEY is not configured");

        String originalName = file.getOriginalFilename() == null ? fallbackName : file.getOriginalFilename();
        String safeName = originalName.replaceAll("[^a-zA-Z0-9._-]", "_");
        String key = folder + "/" + UUID.randomUUID() + "-" + safeName;
        ParsedS3Api parsed = parseS3Api();
        String endpoint = parsed.endpoint();
        String bucket = parsed.bucket();

        try (S3Client s3Client = S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(accessKeyId, secretAccessKey)
                        )
                )
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build()) {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(file.getContentType() == null ? "application/octet-stream" : file.getContentType())
                    // Keys embed a random UUID, so an object never changes: let browsers/CDN cache it for a year.
                    .cacheControl("public, max-age=31536000, immutable")
                    .build();

            s3Client.putObject(request, RequestBody.fromBytes(file.getBytes()));
        } catch (IOException ex) {
            throw new RuntimeException(uploadError, ex);
        }

        if (publicDevelopmentUrl != null && !publicDevelopmentUrl.isBlank()) {
            return publicDevelopmentUrl.replaceAll("/+$", "") + "/" + key;
        }

        return endpoint.replaceAll("/+$", "") + "/" + bucket + "/" + key;
    }

    private ParsedS3Api parseS3Api() {
        URI uri = URI.create(s3Api);
        String path = uri.getPath() == null ? "" : uri.getPath().replaceAll("^/+", "");
        if (path.isBlank()) throw new IllegalStateException("S3_API must include bucket path, e.g. https://.../bucket-name");

        String bucket = path.split("/")[0];
        if (bucket.isBlank()) throw new IllegalStateException("S3_API bucket path is invalid");

        String endpoint = uri.getScheme() + "://" + uri.getHost();
        if (uri.getPort() != -1) endpoint += ":" + uri.getPort();
        return new ParsedS3Api(endpoint, bucket);
    }

    public byte[] download(String resumeUrl) {
        if (resumeUrl == null || resumeUrl.isBlank()) {
            throw new BadRequestException("Resume URL is empty");
        }
        String key = extractKey(resumeUrl);
        ParsedS3Api parsed = parseS3Api();

        try (S3Client s3Client = S3Client.builder()
                .endpointOverride(URI.create(parsed.endpoint()))
                .region(Region.of(region))
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(accessKeyId, secretAccessKey)
                        )
                )
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build()) {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(parsed.bucket())
                    .key(key)
                    .build();
            return s3Client.getObjectAsBytes(request).asByteArray();
        } catch (Exception ex) {
            throw new RuntimeException("Failed to download resume from R2: " + key, ex);
        }
    }

    private String extractKey(String resumeUrl) {
        if (publicDevelopmentUrl != null && !publicDevelopmentUrl.isBlank()) {
            String base = publicDevelopmentUrl.replaceAll("/+$", "") + "/";
            if (resumeUrl.startsWith(base)) {
                return resumeUrl.substring(base.length());
            }
        }
        ParsedS3Api parsed = parseS3Api();
        String prefix = parsed.endpoint().replaceAll("/+$", "") + "/" + parsed.bucket() + "/";
        if (resumeUrl.startsWith(prefix)) {
            return resumeUrl.substring(prefix.length());
        }
        URI uri = URI.create(resumeUrl);
        String path = uri.getPath();
        if (path != null && !path.isBlank()) {
            path = path.replaceAll("^/+", "");
            int slashIdx = path.indexOf('/');
            return slashIdx >= 0 ? path.substring(slashIdx + 1) : path;
        }
        throw new BadRequestException("Could not extract key from resume URL");
    }

    private record ParsedS3Api(String endpoint, String bucket) {}
}
