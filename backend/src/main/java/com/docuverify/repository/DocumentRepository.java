package com.docuverify.repository;

import com.docuverify.entity.Document;
import com.docuverify.entity.Institution;
import com.docuverify.entity.User;
import com.docuverify.enums.DocumentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {
    Page<Document> findByUploadedBy(User user, Pageable pageable);
    Page<Document> findByInstitution(Institution institution, Pageable pageable);
    Page<Document> findByStatus(DocumentStatus status, Pageable pageable);
    Page<Document> findByInstitutionAndStatus(Institution institution, DocumentStatus status, Pageable pageable);
    List<Document> findByStatusAndExpiresAtBefore(DocumentStatus status, LocalDateTime now);
    Optional<Document> findByVerificationToken(String token);
    boolean existsByFileHash(String fileHash);

    long countByUploadedBy(User user);
    long countByUploadedByAndStatus(User user, DocumentStatus status);
    long countByInstitution(Institution institution);
    long countByInstitutionAndStatus(Institution institution, DocumentStatus status);
    long countByStatus(DocumentStatus status);

    // Documents verified by a specific verifier
    long countByVerifiedBy(User verifier);
    long countByVerifiedByAndStatus(User verifier, DocumentStatus status);

    // Verified today by specific verifier
    @Query("SELECT COUNT(d) FROM Document d WHERE d.verifiedBy = :verifier AND d.status = :status AND d.updatedAt >= :since")
    long countByVerifiedByAndStatusSince(User verifier, DocumentStatus status, LocalDateTime since);

    // Urgent: under review for more than 48h
    @Query("SELECT COUNT(d) FROM Document d WHERE d.institution = :institution AND d.status = 'UNDER_REVIEW' AND d.updatedAt < :threshold")
    long countUrgentByInstitution(Institution institution, LocalDateTime threshold);

    @Query("SELECT COUNT(d) FROM Document d WHERE d.status = 'UNDER_REVIEW' AND d.updatedAt < :threshold")
    long countUrgentGlobal(LocalDateTime threshold);

    // Platform-wide verified today
    @Query("SELECT COUNT(d) FROM Document d WHERE d.status = 'APPROVED' AND d.updatedAt >= :since")
    long countApprovedSince(LocalDateTime since);

    @Query("SELECT d.institution.id, COUNT(d) FROM Document d WHERE d.institution.id IN :institutionIds GROUP BY d.institution.id")
    java.util.List<Object[]> countDocumentsByInstitutionIds(@org.springframework.data.repository.query.Param("institutionIds") java.util.List<UUID> institutionIds);
}
