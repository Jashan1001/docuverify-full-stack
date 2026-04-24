package com.docuverify.controller;

import com.docuverify.dto.*;
import com.docuverify.entity.Document;
import com.docuverify.entity.User;
import com.docuverify.enums.Role;
import com.docuverify.exception.ResourceNotFoundException;
import com.docuverify.repository.UserRepository;
import com.docuverify.service.AuditLogService;
import com.docuverify.service.DocumentService;
import com.docuverify.service.VerificationService;
import com.docuverify.util.RequestIpUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
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
    private final UserRepository userRepository;

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

    @GetMapping("/logs/{documentId}")
    public ResponseEntity<ApiResponse<List<VerificationLogResponse>>> getLogs(
            @PathVariable UUID documentId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Document doc = documentService.findByIdRaw(documentId);
        User requester = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        boolean isPrivileged = requester.getRole() == Role.ROLE_VERIFIER
                || requester.getRole() == Role.ROLE_ADMIN
                || requester.getRole() == Role.ROLE_INSTITUTION_ADMIN;

        boolean isOwner = doc.getUploadedBy().getEmail().equals(userDetails.getUsername());

        if (!isPrivileged && !isOwner) {
            throw new AccessDeniedException("You do not have access to this document's audit log");
        }

        List<VerificationLogResponse> logs = auditLogService.getLogsForDocument(doc);
        return ResponseEntity.ok(ApiResponse.success("Audit logs fetched", logs));
    }
}