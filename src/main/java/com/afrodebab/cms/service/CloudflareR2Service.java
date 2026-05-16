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
        if (s3Api == null || s3Api.isBlank()) throw new IllegalStateException("S3_API is not configured");
        if (accessKeyId == null || accessKeyId.isBlank()) throw new IllegalStateException("ACCESS_KEY_ID is not configured");
        if (secretAccessKey == null || secretAccessKey.isBlank()) throw new IllegalStateException("SECRET_ACCESS_KEY is not configured");

        String originalName = file.getOriginalFilename() == null ? "photo" : file.getOriginalFilename();
        String safeName = originalName.replaceAll("[^a-zA-Z0-9._-]", "_");
        String key = "employees/" + employeeId + "/" + UUID.randomUUID() + "-" + safeName;
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
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(request, RequestBody.fromBytes(file.getBytes()));
        } catch (IOException ex) {
            throw new RuntimeException("Failed to upload employee photo", ex);
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

    private record ParsedS3Api(String endpoint, String bucket) {}
}
