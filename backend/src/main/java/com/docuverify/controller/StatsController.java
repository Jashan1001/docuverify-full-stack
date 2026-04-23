package com.docuverify.controller;

import com.docuverify.dto.*;
import com.docuverify.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @GetMapping("/user")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserStatsResponse>> userStats(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success("Stats fetched",
                statsService.getUserStats(userDetails.getUsername())));
    }

    @GetMapping("/verifier")
    @PreAuthorize("hasAnyRole('VERIFIER', 'ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<VerifierStatsResponse>> verifierStats(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success("Stats fetched",
                statsService.getVerifierStats(userDetails.getUsername())));
    }

    @GetMapping("/institution")
    @PreAuthorize("hasAnyRole('INSTITUTION_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<InstitutionStatsResponse>> institutionStats(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success("Stats fetched",
                statsService.getInstitutionStats(userDetails.getUsername())));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AdminStatsResponse>> adminStats() {
        return ResponseEntity.ok(ApiResponse.success("Stats fetched",
                statsService.getAdminStats()));
    }
}
