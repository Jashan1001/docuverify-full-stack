package com.docuverify.controller;

import com.docuverify.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.MalformedURLException;
import java.nio.file.Path;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Slf4j
public class FileController {

    private final StorageService storageService;
    private final com.docuverify.repository.UserRepository userRepository;

    @GetMapping("/{institutionId}/{filename:.+}")
    public ResponseEntity<Resource> serveFile(
            @PathVariable String institutionId,
            @PathVariable String filename,
            java.security.Principal principal
    ) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        com.docuverify.entity.User user = userRepository.findByEmail(principal.getName())
                .orElse(null);

        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        boolean isAdmin = user.getRole() == com.docuverify.enums.Role.ROLE_ADMIN;
        boolean matchesInstitution = user.getInstitution() != null && 
                                     user.getInstitution().getId().toString().equals(institutionId);

        if (!isAdmin && !matchesInstitution) {
            log.warn("Unauthorized file access attempt by {} for institution {}", principal.getName(), institutionId);
            return ResponseEntity.status(403).build();
        }

        try {
            Path filePath = storageService.resolveFilePath(institutionId, filename);
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            String contentType = determineContentType(filename);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);

        } catch (MalformedURLException e) {
            log.error("Error serving file: {}", filename, e);
            return ResponseEntity.badRequest().build();
        }
    }

    private String determineContentType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".pdf")) return "application/pdf";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".doc")) return "application/msword";
        if (lower.endsWith(".docx"))
            return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        return "application/octet-stream";
    }
}
