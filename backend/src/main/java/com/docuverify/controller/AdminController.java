package com.docuverify.controller;

import com.docuverify.dto.*;
import com.docuverify.enums.Role;
import com.docuverify.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    // ── INSTITUTIONS ──────────────────────────────────────────────────────────

    @GetMapping("/institutions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<InstitutionResponse>>> getInstitutions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.success("Institutions fetched",
                adminService.getAllInstitutions(pageable)));
    }

    @PostMapping("/institutions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<InstitutionResponse>> createInstitution(
            @Valid @RequestBody CreateInstitutionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Institution created",
                        adminService.createInstitution(request)));
    }

    @PatchMapping("/institutions/{id}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<InstitutionResponse>> toggleInstitution(
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Institution status updated",
                adminService.toggleInstitutionStatus(id)));
    }

    // ── USERS ─────────────────────────────────────────────────────────────────

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String role) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<UserResponse> users = role != null
                ? adminService.getUsersByRole(Role.valueOf(role), pageable)
                : adminService.getAllUsers(pageable);
        return ResponseEntity.ok(ApiResponse.success("Users fetched", users));
    }

    @PatchMapping("/users/{id}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> toggleUser(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("User status updated",
                adminService.toggleUserStatus(id)));
    }

    @PostMapping("/users/assign-role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> assignRole(
            @Valid @RequestBody AssignRoleRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Role assigned",
                adminService.assignRole(request)));
    }

    // ── INSTITUTION ADMIN: their own team ─────────────────────────────────────

    @GetMapping("/institution/members")
    @PreAuthorize("hasAnyRole('INSTITUTION_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> getMyInstitutionMembers(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.success("Members fetched",
                adminService.getInstitutionMembers(userDetails.getUsername(), pageable)));
    }
}
