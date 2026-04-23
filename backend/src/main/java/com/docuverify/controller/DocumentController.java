package com.docuverify.controller;

import com.docuverify.dto.ApiResponse;
import com.docuverify.dto.DocumentRequest;
import com.docuverify.dto.DocumentResponse;
import com.docuverify.enums.DocumentStatus;
import com.docuverify.service.DocumentService;
import com.docuverify.util.RequestIpUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    /**
     * Upload a new document with metadata + file.
     * Multipart: "file" + JSON fields title/description.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<DocumentResponse>> upload(
            @RequestPart("file") MultipartFile file,
            @RequestPart("title") String title,
            @RequestPart(value = "description", required = false) String description,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpRequest
    ) throws Exception {
        DocumentRequest request = new DocumentRequest();
        request.setTitle(title);
        request.setDescription(description);

        DocumentResponse response = documentService.uploadDocument(
                request, file, userDetails.getUsername(), RequestIpUtil.getClientIp(httpRequest));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Document uploaded successfully", response));
    }

    /**
     * Get all documents uploaded by the authenticated user.
     */
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<Page<DocumentResponse>>> getMyDocuments(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<DocumentResponse> docs = documentService.getMyDocuments(userDetails.getUsername(), pageable);
        return ResponseEntity.ok(ApiResponse.success("Documents fetched", docs));
    }

    /**
     * Get a single document by ID (owner or verifier/admin).
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DocumentResponse>> getById(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        DocumentResponse doc = documentService.getDocumentById(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Document fetched", doc));
    }

    /**
     * Submit a document for review (UPLOADED → UNDER_REVIEW).
     */
    @PatchMapping("/{id}/submit")
    public ResponseEntity<ApiResponse<Void>> submit(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpRequest
    ) {
        documentService.submitForReview(id, userDetails.getUsername(), RequestIpUtil.getClientIp(httpRequest));
        return ResponseEntity.ok(ApiResponse.success("Document submitted for review", null));
    }

    /**
     * Delete a document (only owner, non-APPROVED docs).
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        documentService.deleteDocument(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Document deleted", null));
    }

    /**
     * Get documents pending review — for verifiers.
     */
    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<Page<DocumentResponse>>> getPending(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").ascending());
        Page<DocumentResponse> docs = documentService.getPendingDocuments(userDetails.getUsername(), pageable);
        return ResponseEntity.ok(ApiResponse.success("Pending documents fetched", docs));
    }

    /**
     * Get documents for authenticated user's institution.
     * Optional status filter.
     */
    @GetMapping("/institution")
    @PreAuthorize("hasAnyRole('VERIFIER', 'INSTITUTION_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<Page<DocumentResponse>>> getInstitutionDocuments(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        DocumentStatus statusFilter = null;
        if (status != null && !status.isBlank()) {
            statusFilter = DocumentStatus.valueOf(status);
        }
        Page<DocumentResponse> docs = documentService.getInstitutionDocuments(
                userDetails.getUsername(), pageable, statusFilter
        );
        return ResponseEntity.ok(ApiResponse.success("Institution documents fetched", docs));
    }
}
