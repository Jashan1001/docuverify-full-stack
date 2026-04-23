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
        Path basePath = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path resolvedPath = basePath.resolve(institutionId).resolve(filename).normalize();
        
        if (!resolvedPath.startsWith(basePath)) {
            throw new SecurityException("Path traversal attempt detected");
        }
        return resolvedPath;
    }

    public String computeSha256(MultipartFile file) throws IOException, NoSuchAlgorithmException {
        return computeSha256(file.getBytes());
    }

    public String computeSha256(Path filePath) throws IOException, NoSuchAlgorithmException {
        return computeSha256(Files.readAllBytes(filePath));
    }

    private String computeSha256(byte[] bytes) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(bytes);
        return HexFormat.of().formatHex(hash);
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf('.'));
    }
}
