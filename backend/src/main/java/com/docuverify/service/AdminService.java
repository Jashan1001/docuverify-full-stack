package com.docuverify.service;

import com.docuverify.dto.*;
import com.docuverify.entity.Institution;
import com.docuverify.entity.User;
import com.docuverify.enums.Role;
import com.docuverify.exception.DuplicateResourceException;
import com.docuverify.exception.ResourceNotFoundException;
import com.docuverify.repository.DocumentRepository;
import com.docuverify.repository.InstitutionRepository;
import com.docuverify.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final UserRepository userRepository;
    private final InstitutionRepository institutionRepository;
    private final DocumentRepository documentRepository;

    // ── INSTITUTION MANAGEMENT ────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<InstitutionResponse> getAllInstitutions(Pageable pageable) {
        return institutionRepository.findAll(pageable).map(this::toInstitutionResponse);
    }

    @Transactional
    public InstitutionResponse createInstitution(CreateInstitutionRequest request) {
        if (institutionRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Institution already exists: " + request.getName());
        }
        if (institutionRepository.existsByDomain(request.getDomain())) {
            throw new DuplicateResourceException("Domain already registered: " + request.getDomain());
        }
        Institution institution = Institution.builder()
                .name(request.getName())
                .domain(request.getDomain())
                .contactEmail(request.getContactEmail())
                .active(true)
                .build();
        Institution saved = institutionRepository.save(institution);
        log.info("Institution created: {}", saved.getName());
        return toInstitutionResponse(saved);
    }

    @Transactional
    public InstitutionResponse toggleInstitutionStatus(UUID institutionId) {
        Institution institution = institutionRepository.findById(institutionId)
                .orElseThrow(() -> new ResourceNotFoundException("Institution not found"));
        institution.setActive(!institution.isActive());
        institutionRepository.save(institution);
        log.info("Institution {} status toggled to {}", institution.getName(), institution.isActive());
        return toInstitutionResponse(institution);
    }

    // ── USER MANAGEMENT ───────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(this::toUserResponse);
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> getUsersByRole(Role role, Pageable pageable) {
        return userRepository.findByRole(role, pageable).map(this::toUserResponse);
    }

    @Transactional
    public UserResponse toggleUserStatus(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getRole() == Role.ROLE_ADMIN) {
            throw new IllegalStateException("Cannot disable the platform admin");
        }
        user.setEnabled(!user.isEnabled());
        userRepository.save(user);
        log.info("User {} status toggled to {}", user.getEmail(), user.isEnabled());
        return toUserResponse(user);
    }

    // ── ROLE ASSIGNMENT ───────────────────────────────────────────────────────
    // Only ADMIN can call this. Assigns VERIFIER or INSTITUTION_ADMIN roles.

    @Transactional
    public UserResponse assignRole(AssignRoleRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (request.getRole() == Role.ROLE_ADMIN) {
            throw new IllegalStateException("Cannot assign ADMIN role via API");
        }

        Institution institution = null;
        if (request.getInstitutionId() != null) {
            institution = institutionRepository.findById(request.getInstitutionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Institution not found"));
        } else if (request.getRole() == Role.ROLE_VERIFIER || request.getRole() == Role.ROLE_INSTITUTION_ADMIN) {
            throw new IllegalArgumentException("Institution ID required for this role");
        }

        user.setRole(request.getRole());
        user.setInstitution(institution);
        userRepository.save(user);

        log.info("Assigned {} role to user {} (institution: {})",
                request.getRole(), user.getEmail(),
                institution != null ? institution.getName() : "none");

        return toUserResponse(user);
    }

    // ── INSTITUTION ADMIN: their own institution's users ──────────────────────

    @Transactional(readOnly = true)
    public Page<UserResponse> getInstitutionMembers(String adminEmail, Pageable pageable) {
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));
        if (admin.getInstitution() == null) {
            throw new ResourceNotFoundException("No institution assigned");
        }
        return userRepository.findByInstitution(admin.getInstitution(), pageable)
                .map(this::toUserResponse);
    }

    // ── MAPPERS ───────────────────────────────────────────────────────────────

    private InstitutionResponse toInstitutionResponse(Institution i) {
        return InstitutionResponse.builder()
                .id(i.getId())
                .name(i.getName())
                .domain(i.getDomain())
                .contactEmail(i.getContactEmail())
                .active(i.isActive())
                .userCount(userRepository.countByInstitution(i))
                .documentCount(documentRepository.countByInstitution(i))
                .createdAt(i.getCreatedAt())
                .build();
    }

    public UserResponse toUserResponse(User u) {
        return UserResponse.builder()
                .id(u.getId())
                .fullName(u.getFullName())
                .email(u.getEmail())
                .role(u.getRole())
                .enabled(u.isEnabled())
                .institutionName(u.getInstitution() != null ? u.getInstitution().getName() : null)
                .institutionId(u.getInstitution() != null ? u.getInstitution().getId() : null)
                .createdAt(u.getCreatedAt())
                .build();
    }
}
