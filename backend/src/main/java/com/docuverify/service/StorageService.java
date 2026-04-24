package com.docuverify.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

@Service
@Slf4j
public class StorageService {

    private final S3Client s3Client;

    @Value("${storage.mode:local}")
    private String storageMode;

    @Value("${storage.local.upload-dir:./uploads}")
    private String uploadDir;

    @Value("${aws.s3.bucket:docuverify-documents}")
    private String s3Bucket;

    // ✅ OPTIONAL injection (key fix)
    public StorageService(@Autowired(required = false) S3Client s3Client) {
        this.s3Client = s3Client;
    }

    // ── Public API ───────────────────────────────────────────────────────────

    public String uploadFile(MultipartFile file, String institutionId) throws IOException {
        String extension = getExtension(file.getOriginalFilename());
        String storedName = UUID.randomUUID() + extension;

        if (isS3()) {
            return uploadToS3(file, institutionId, storedName);
        } else {
            return uploadToLocal(file, institutionId, storedName);
        }
    }

    public void deleteFile(String fileUrl) {
        if (isS3()) {
            deleteFromS3(fileUrl);
        } else {
            deleteFromLocal(fileUrl);
        }
    }

    public byte[] readFileBytes(String fileUrl) throws IOException {
        if (isS3()) {
            return readFromS3(s3KeyFromUrl(fileUrl));
        } else {
            String relativePath = fileUrl.replace("/api/files/", "");
            Path filePath = Paths.get(uploadDir, relativePath);
            return Files.readAllBytes(filePath);
        }
    }

    public Path resolveFilePath(String institutionId, String filename) {
        return Paths.get(uploadDir, institutionId, filename);
    }

    public String computeSha256(MultipartFile file) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(file.getBytes());
        return HexFormat.of().formatHex(hash);
    }

    public String computeSha256FromBytes(byte[] bytes) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(bytes));
    }

    // ── Local implementation ─────────────────────────────────────────────────

    private String uploadToLocal(MultipartFile file, String institutionId, String storedName) throws IOException {
        Path uploadPath = Paths.get(uploadDir, institutionId);
        Files.createDirectories(uploadPath);

        Path targetPath = uploadPath.resolve(storedName);
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        String fileUrl = "/api/files/" + institutionId + "/" + storedName;
        log.info("File stored locally: {}", fileUrl);
        return fileUrl;
    }

    private void deleteFromLocal(String fileUrl) {
        try {
            String relativePath = fileUrl.replace("/api/files/", "");
            Path filePath = Paths.get(uploadDir, relativePath);
            Files.deleteIfExists(filePath);
            log.info("Local file deleted: {}", fileUrl);
        } catch (IOException e) {
            log.error("Failed to delete local file: {}", fileUrl, e);
        }
    }

    // ── S3 implementation ────────────────────────────────────────────────────

    private String uploadToS3(MultipartFile file, String institutionId, String storedName) throws IOException {
        ensureS3();

        String s3Key = "documents/" + institutionId + "/" + storedName;

        try (InputStream inputStream = file.getInputStream()) {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(s3Bucket)
                    .key(s3Key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(request, RequestBody.fromInputStream(inputStream, file.getSize()));
        }

        String fileUrl = "/api/files/s3/" + s3Key;
        log.info("File uploaded to S3: s3://{}/{}", s3Bucket, s3Key);
        return fileUrl;
    }

    private void deleteFromS3(String fileUrl) {
        ensureS3();

        try {
            String s3Key = s3KeyFromUrl(fileUrl);

            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(s3Bucket)
                    .key(s3Key)
                    .build());

            log.info("S3 file deleted: {}", s3Key);
        } catch (Exception e) {
            log.error("Failed to delete S3 file: {}", fileUrl, e);
        }
    }

    private byte[] readFromS3(String s3Key) throws IOException {
        ensureS3();

        try {
            ResponseInputStream<GetObjectResponse> response =
                    s3Client.getObject(GetObjectRequest.builder()
                            .bucket(s3Bucket)
                            .key(s3Key)
                            .build());

            return response.readAllBytes();
        } catch (S3Exception e) {
            throw new IOException("Failed to read S3 object: " + s3Key, e);
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private boolean isS3() {
        return "s3".equalsIgnoreCase(storageMode) && s3Client != null;
    }

    private void ensureS3() {
        if (s3Client == null) {
            throw new IllegalStateException("S3 is not configured but storage.mode=s3");
        }
    }

    private String s3KeyFromUrl(String fileUrl) {
        return fileUrl.replace("/api/files/s3/", "");
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf('.'));
    }
}