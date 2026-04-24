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
            Map uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "docuverify/" + institutionId,
                            "resource_type", "auto"
                    )
            );

            String url = uploadResult.get("secure_url").toString();
            log.info("File uploaded to Cloudinary: {}", url);

            return url;

        } catch (Exception e) {
            throw new RuntimeException("File upload failed", e);
        }
    }

    // ✅ Delete file (optional)
    public void deleteFile(String fileUrl) {
        try {
            String publicId = extractPublicId(fileUrl);

            cloudinary.uploader().destroy(
                    publicId,
                    ObjectUtils.emptyMap()
            );

            log.info("Deleted from Cloudinary: {}", publicId);

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

    // 🔧 helper
    private String extractPublicId(String url) {
        String[] parts = url.split("/");
        String fileWithExt = parts[parts.length - 1];
        return fileWithExt.substring(0, fileWithExt.lastIndexOf("."));
    }
}