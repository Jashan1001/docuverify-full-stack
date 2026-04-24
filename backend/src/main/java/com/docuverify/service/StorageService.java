package com.docuverify.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;

@Service
@Slf4j
public class StorageService {

    private final Cloudinary cloudinary;

    public StorageService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    // ✅ Upload file to Cloudinary
    public String uploadFile(MultipartFile file, String institutionId) {
        try {
            // Use 'raw' for PDFs to avoid Cloudinary's image processing which can fail for some documents
            String resourceType = "auto";
            String contentType = file.getContentType();
            if (contentType != null && contentType.equalsIgnoreCase("application/pdf")) {
                resourceType = "raw";
            }

            Map uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "docuverify/" + institutionId,
                            "resource_type", resourceType,
                            "use_filename", true,
                            "unique_filename", true
                    )
            );

            String url = uploadResult.get("secure_url").toString();
            log.info("File uploaded to Cloudinary ({}): {}", resourceType, url);

            return url;

        } catch (Exception e) {
            log.error("Cloudinary upload failed", e);
            throw new RuntimeException("File upload failed", e);
        }
    }

    // ✅ Delete file (optional)
    public void deleteFile(String fileUrl) {
        try {
            // Improved publicId extraction to handle folders and resource types
            String publicId = extractPublicId(fileUrl);
            String resourceType = fileUrl.contains("/raw/") ? "raw" : "image";

            cloudinary.uploader().destroy(
                    publicId,
                    ObjectUtils.asMap("resource_type", resourceType)
            );

            log.info("Deleted from Cloudinary ({}): {}", resourceType, publicId);

        } catch (Exception e) {
            log.error("Failed to delete file", e);
        }
    }

    // ✅ Read bytes from URL (for tamper detection)
    public byte[] readFileBytes(String fileUrl) {
        try {
            URL url = new URL(fileUrl);
            try (InputStream in = url.openStream()) {
                return in.readAllBytes();
            }
        } catch (Exception e) {
            log.error("Failed to read bytes from URL: {}", fileUrl, e);
            throw new RuntimeException("Could not read file from storage", e);
        }
    }

    // ✅ SHA256 (unchanged)
    public String computeSha256(MultipartFile file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(file.getBytes());
        return HexFormat.of().formatHex(hash);
    }

    public String computeSha256FromBytes(byte[] bytes) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(bytes));
    }

    // 🔧 helper to extract the full public ID including folders
    private String extractPublicId(String url) {
        // Example: https://res.cloudinary.com/demo/image/upload/v1234/folder/sub/id.pdf
        // We need 'folder/sub/id'
        String[] parts = url.split("/upload/");
        if (parts.length < 2) return url;
        
        String pathAfterUpload = parts[1]; // v1234/folder/sub/id.pdf
        String pathWithoutVersion = pathAfterUpload.substring(pathAfterUpload.indexOf("/") + 1); // folder/sub/id.pdf
        
        int dotIndex = pathWithoutVersion.lastIndexOf(".");
        if (dotIndex > 0) {
            return pathWithoutVersion.substring(0, dotIndex);
        }
        return pathWithoutVersion;
    }
}