package com.docuverify.controller;

import com.docuverify.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
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

    /**
     * S3 mode — path: /api/files/s3/documents/{institutionId}/{filename}
     */
    @GetMapping("/s3/**")
    public ResponseEntity<Resource> serveS3File(
            @RequestParam(required = false) String filename
    ) {
        // Extract full S3 key from request URI — handled by StorageService
        // The full URL is passed directly from the stored fileUrl
        return ResponseEntity.notFound().build(); // placeholder — real serving below
    }

    /**
     * Unified file serving — handles both local and S3 via stored fileUrl format.
     * Local: /api/files/{institutionId}/{filename}
     * S3:    /api/files/s3/documents/{institutionId}/{filename}
     */
    @GetMapping({"/{institutionId}/{filename:.+}", "/s3/documents/{institutionId}/{filename:.+}"})
    public ResponseEntity<Resource> serveFile(
            @PathVariable String institutionId,
            @PathVariable String filename,
            @RequestParam(required = false, defaultValue = "false") boolean s3
    ) {
        try {
            String contentType = determineContentType(filename);

            // Determine if this is an S3 request by path prefix
            // StorageService.readFileBytes handles routing internally
            String fileUrl = s3
                    ? "/api/files/s3/documents/" + institutionId + "/" + filename
                    : "/api/files/" + institutionId + "/" + filename;

            try {
                byte[] bytes = storageService.readFileBytes(fileUrl);
                ByteArrayResource resource = new ByteArrayResource(bytes);
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "inline; filename=\"" + filename + "\"")
                        .contentLength(bytes.length)
                        .body(resource);
            } catch (Exception e) {
                // Fall back to local file serving
                Path filePath = storageService.resolveFilePath(institutionId, filename);
                Resource resource = new UrlResource(filePath.toUri());
                if (!resource.exists() || !resource.isReadable()) {
                    return ResponseEntity.notFound().build();
                }
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "inline; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            }
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