package com.docuverify.service;

import com.docuverify.dto.*;
import com.docuverify.entity.Institution;
import com.docuverify.entity.User;
import com.docuverify.enums.DocumentStatus;
import com.docuverify.enums.Role;
import com.docuverify.exception.ResourceNotFoundException;
import com.docuverify.repository.DocumentRepository;
import com.docuverify.repository.InstitutionRepository;
import com.docuverify.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class StatsService {

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final InstitutionRepository institutionRepository;

    // ── ROLE_USER ─────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public UserStatsResponse getUserStats(String email) {
        User user = findUser(email);
        return UserStatsResponse.builder()
                .total(documentRepository.countByUploadedBy(user))
                .approved(documentRepository.countByUploadedByAndStatus(user, DocumentStatus.APPROVED))
                .underReview(documentRepository.countByUploadedByAndStatus(user, DocumentStatus.UNDER_REVIEW))
                .rejected(documentRepository.countByUploadedByAndStatus(user, DocumentStatus.REJECTED))
                .uploaded(documentRepository.countByUploadedByAndStatus(user, DocumentStatus.UPLOADED))
                .build();
    }

    // ── ROLE_VERIFIER ─────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public VerifierStatsResponse getVerifierStats(String email) {
        User verifier = findUser(email);
        Institution institution = verifier.getInstitution();

        long queueSize = institution != null
                ? documentRepository.countByInstitutionAndStatus(institution, DocumentStatus.UNDER_REVIEW)
                : documentRepository.countByStatus(DocumentStatus.UNDER_REVIEW);

        LocalDateTime todayStart = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime urgentThreshold = LocalDateTime.now().minusHours(48);

        long urgentCount = institution != null
                ? documentRepository.countUrgentByInstitution(institution, DocumentStatus.UNDER_REVIEW, urgentThreshold)
                : documentRepository.countUrgentGlobal(DocumentStatus.UNDER_REVIEW, urgentThreshold);

        return VerifierStatsResponse.builder()
                .queueSize(queueSize)
                .totalApproved(documentRepository.countByVerifiedByAndStatus(verifier, DocumentStatus.APPROVED))
                .totalRejected(documentRepository.countByVerifiedByAndStatus(verifier, DocumentStatus.REJECTED))
                .approvedToday(documentRepository.countByVerifiedByAndStatusSince(verifier, DocumentStatus.APPROVED, todayStart))
                .rejectedToday(documentRepository.countByVerifiedByAndStatusSince(verifier, DocumentStatus.REJECTED, todayStart))
                .urgentCount(urgentCount)
                .build();
    }

    // ── ROLE_INSTITUTION_ADMIN ────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public InstitutionStatsResponse getInstitutionStats(String email) {
        User admin = findUser(email);
        Institution institution = admin.getInstitution();
        if (institution == null) throw new ResourceNotFoundException("No institution assigned to this admin");

        return InstitutionStatsResponse.builder()
                .institutionName(institution.getName())
                .totalDocuments(documentRepository.countByInstitution(institution))
                .approvedDocuments(documentRepository.countByInstitutionAndStatus(institution, DocumentStatus.APPROVED))
                .pendingDocuments(documentRepository.countByInstitutionAndStatus(institution, DocumentStatus.UNDER_REVIEW))
                .rejectedDocuments(documentRepository.countByInstitutionAndStatus(institution, DocumentStatus.REJECTED))
                .totalMembers(userRepository.countByInstitution(institution))
                .verifierCount(userRepository.countByInstitutionAndRole(institution, Role.ROLE_VERIFIER))
                .build();
    }

    // ── ROLE_ADMIN ────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public AdminStatsResponse getAdminStats() {
        LocalDateTime todayStart = LocalDateTime.now().toLocalDate().atStartOfDay();

        return AdminStatsResponse.builder()
                .totalInstitutions(institutionRepository.count())
                .activeInstitutions(institutionRepository.countByActive(true))
                .totalUsers(userRepository.count())
                .totalDocuments(documentRepository.count())
                .approvedDocuments(documentRepository.countByStatus(DocumentStatus.APPROVED))
                .pendingDocuments(documentRepository.countByStatus(DocumentStatus.UNDER_REVIEW))
                .verifiedToday(documentRepository.countApprovedSince(DocumentStatus.APPROVED, todayStart))
                .build();
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }
}
