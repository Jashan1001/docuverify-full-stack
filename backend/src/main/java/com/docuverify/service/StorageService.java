package com.docuverify.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

@Service
@Slf4j
public class StorageService {

    @Value("${storage.local.upload-dir:./uploads}")
    private String uploadDir;

    public String uploadFile(MultipartFile file, String institutionId) throws IOException {
        Path uploadPath = Paths.get(uploadDir, institutionId);
        Files.createDirectories(uploadPath);
        String extension = getExtension(file.getOriginalFilename());
        String storedName = UUID.randomUUID() + extension;
        Path targetPath = uploadPath.resolve(storedName);
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        String fileUrl = "/api/files/" + institutionId + "/" + storedName;
        log.info("File stored locally: {}", fileUrl);
        return fileUrl;
    }

    public void deleteFile(String fileUrl) {
        try {
            String relativePath = fileUrl.replace("/api/files/", "");
            Path filePath = Paths.get(uploadDir, relativePath);
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.error("Failed to delete file: {}", fileUrl, e);
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

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf('.'));
    }
}
