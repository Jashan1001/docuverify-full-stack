package com.docuverify.controller;

import com.docuverify.dto.ApiResponse;
import com.docuverify.dto.PublicVerificationResponse;
import com.docuverify.service.VerificationService;
import com.docuverify.util.RequestIpUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicController {

    private final VerificationService verificationService;

    /**
     * Public document verification — no authentication required.
     * Called by QR code scans or shared verification links.
     *
     * GET /api/public/verify/{token}
     */
    @GetMapping("/verify/{token}")
    public ResponseEntity<ApiResponse<PublicVerificationResponse>> verify(
            @PathVariable String token,
            HttpServletRequest request
    ) {
        String clientIp = RequestIpUtil.getClientIp(request);
        PublicVerificationResponse response = verificationService.verifyPublicly(token, clientIp);
        return ResponseEntity.ok(ApiResponse.success("Verification complete", response));
    }

    /**
     * Health check — useful for load balancer probes.
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(ApiResponse.success("DocuVerify is running", "OK"));
    }
}
