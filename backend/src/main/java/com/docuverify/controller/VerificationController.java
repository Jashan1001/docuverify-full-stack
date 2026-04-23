package com.docuverify.controller;

import com.docuverify.dto.ApiResponse;
import com.docuverify.dto.DocumentResponse;
import com.docuverify.dto.VerificationRequest;
import com.docuverify.service.AuditLogService;
import com.docuverify.service.DocumentService;
import com.docuverify.service.VerificationService;
import com.docuverify.util.RequestIpUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/verification")
@RequiredArgsConstructor
public class VerificationController {

    private final VerificationService verificationService;
    private final AuditLogService auditLogService;
    private final DocumentService documentService;

    /**
     * Approve a document — verifiers and admins only.
     * State transition: UNDER_REVIEW → APPROVED
     */
    @PostMapping("/approve")
    @PreAuthorize("hasAnyRole('VERIFIER', 'ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<DocumentResponse>> approve(
            @Valid @RequestBody VerificationRequest request,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpRequest
    ) {
        DocumentResponse response = verificationService.approveDocument(
                request, userDetails.getUsername(), RequestIpUtil.getClientIp(httpRequest));
        return ResponseEntity.ok(ApiResponse.success("Document approved successfully", response));
    }

    /**
     * Reject a document — verifiers and admins only.
     * State transition: UNDER_REVIEW → REJECTED
     */
    @PostMapping("/reject")
    @PreAuthorize("hasAnyRole('VERIFIER', 'ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<DocumentResponse>> reject(
            @Valid @RequestBody VerificationRequest request,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpRequest
    ) {
        if (request.getRejectionReason() == null || request.getRejectionReason().isBlank()) {
            throw new IllegalArgumentException("Rejection reason is required");
        }
        DocumentResponse response = verificationService.rejectDocument(
                request, userDetails.getUsername(), RequestIpUtil.getClientIp(httpRequest));
        return ResponseEntity.ok(ApiResponse.success("Document rejected", response));
    }

    /**
     * Get full audit trail for a document.
     */
    @GetMapping("/logs/{documentId}")
    @PreAuthorize("hasAnyRole('VERIFIER', 'ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<List<com.docuverify.dto.VerificationLogResponse>>> getLogs(
            @PathVariable UUID documentId
    ) {
        var doc = documentService.findByIdRaw(documentId);
        List<com.docuverify.dto.VerificationLogResponse> logs = auditLogService.getLogsForDocument(doc);
        return ResponseEntity.ok(ApiResponse.success("Audit logs fetched", logs));
    }
}
