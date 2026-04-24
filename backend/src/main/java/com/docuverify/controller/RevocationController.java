package com.docuverify.controller;

import com.docuverify.dto.ApiResponse;
import com.docuverify.dto.DocumentResponse;
import com.docuverify.service.RevocationService;
import com.docuverify.util.RequestIpUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class RevocationController {

    private final RevocationService revocationService;

    /**
     * POST /api/documents/{id}/revoke
     * Body: { "reason": "Certificate was issued in error" }
     */
    @PostMapping("/{id}/revoke")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<DocumentResponse>> revoke(
            @PathVariable UUID id,
            @RequestParam String reason,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest request
    ) {
        DocumentResponse response = revocationService.revokeDocument(
                id, reason, userDetails.getUsername(),
                RequestIpUtil.getClientIp(request));
        return ResponseEntity.ok(ApiResponse.success("Document revoked", response));
    }

    /**
     * PATCH /api/documents/{id}/expiry
     * Param: expiresAt (ISO datetime, null to clear)
     * Example: ?expiresAt=2026-12-31T23:59:59
     */
    @PatchMapping("/{id}/expiry")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'VERIFIER')")
    public ResponseEntity<ApiResponse<DocumentResponse>> setExpiry(
            @PathVariable UUID id,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime expiresAt,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest request
    ) {
        DocumentResponse response = revocationService.setExpiry(
                id, expiresAt, userDetails.getUsername(),
                RequestIpUtil.getClientIp(request));
        return ResponseEntity.ok(ApiResponse.success("Expiry updated", response));
    }
}