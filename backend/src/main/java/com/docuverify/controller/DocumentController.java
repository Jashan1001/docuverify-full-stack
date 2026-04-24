package com.docuverify.controller;

import com.docuverify.dto.ApiResponse;
import com.docuverify.dto.DocumentRequest;
import com.docuverify.dto.DocumentResponse;
import com.docuverify.entity.Document;
import com.docuverify.enums.DocumentStatus;
import com.docuverify.service.DocumentService;
import com.docuverify.util.RequestIpUtil;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Slf4j
public class DocumentController {

    private final DocumentService documentService;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendBaseUrl;

    @PostMapping
    public ResponseEntity<ApiResponse<DocumentResponse>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest request
    ) throws Exception {
        DocumentRequest docRequest = new DocumentRequest(title, description);
        DocumentResponse response = documentService.uploadDocument(
                docRequest, file, userDetails.getUsername(),
                RequestIpUtil.getClientIp(request));
        return ResponseEntity.ok(ApiResponse.success("Document uploaded successfully", response));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<Page<DocumentResponse>>> getMyDocuments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Page<DocumentResponse> docs = documentService.getMyDocuments(
                userDetails.getUsername(),
                PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return ResponseEntity.ok(ApiResponse.success("Documents fetched", docs));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DocumentResponse>> getById(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Document fetched",
                documentService.getDocumentById(id, userDetails.getUsername())));
    }

    @PatchMapping("/{id}/submit")
    public ResponseEntity<ApiResponse<Void>> submitForReview(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest request
    ) {
        documentService.submitForReview(id, userDetails.getUsername(),
                RequestIpUtil.getClientIp(request));
        return ResponseEntity.ok(ApiResponse.success("Operation successful", null));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        documentService.deleteDocument(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Operation successful", null));
    }

    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<Page<DocumentResponse>>> getPending(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Page<DocumentResponse> docs = documentService.getPendingDocuments(
                userDetails.getUsername(),
                PageRequest.of(page, size, Sort.by("createdAt").ascending()));
        return ResponseEntity.ok(ApiResponse.success("Documents fetched", docs));
    }

    @GetMapping("/institution")
    public ResponseEntity<ApiResponse<Page<DocumentResponse>>> getInstitutionDocuments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        DocumentStatus docStatus = null;
        if (status != null && !status.isBlank()) {
            docStatus = DocumentStatus.valueOf(status);
        }
        Page<DocumentResponse> docs = documentService.getInstitutionDocuments(
                userDetails.getUsername(),
                PageRequest.of(page, size, Sort.by("createdAt").descending()),
                docStatus);
        return ResponseEntity.ok(ApiResponse.success("Documents fetched", docs));
    }

    /**
     * Returns a PNG QR code that encodes the public verification URL.
     * Only works for APPROVED documents that have a verificationToken.
     * Requires authentication — same access rules as getById.
     */
    @GetMapping(value = "/{id}/qr", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getQrCode(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails
    ) throws Exception {

        // Reuse existing access control — throws AccessDeniedException if not allowed
        DocumentResponse doc = documentService.getDocumentById(id, userDetails.getUsername());

        if (doc.getVerificationToken() == null) {
            return ResponseEntity.badRequest().build();
        }

        String verifyUrl = frontendBaseUrl + "/verify/" + doc.getVerificationToken();

        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix matrix = writer.encode(verifyUrl, BarcodeFormat.QR_CODE, 300, 300);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", out);

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(out.toByteArray());
    }
}