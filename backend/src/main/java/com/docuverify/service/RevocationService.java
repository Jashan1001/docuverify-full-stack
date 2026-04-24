package com.docuverify.service;

import com.docuverify.dto.DocumentResponse;
import com.docuverify.entity.Document;
import com.docuverify.entity.User;
import com.docuverify.enums.AuditAction;
import com.docuverify.enums.DocumentStatus;
import com.docuverify.enums.Role;
import com.docuverify.exception.InvalidStateTransitionException;
import com.docuverify.exception.ResourceNotFoundException;
import com.docuverify.repository.DocumentRepository;
import com.docuverify.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RevocationService {

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final DocumentService documentService;

    /**
     * Manually revoke an approved document.
     * Only ADMIN or INSTITUTION_ADMIN can revoke.
     * State machine: APPROVED → REVOKED
     */
    @Transactional
    public DocumentResponse revokeDocument(UUID documentId, String reason,
                                            String revokerEmail, String clientIp) {
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));

        if (doc.getStatus() != DocumentStatus.APPROVED) {
            throw new InvalidStateTransitionException(
                    "Only APPROVED documents can be revoked. Current status: " + doc.getStatus());
        }

        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Revocation reason is required");
        }

        User revoker = userRepository.findByEmail(revokerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        boolean canRevoke = revoker.getRole() == Role.ROLE_ADMIN
                || revoker.getRole() == Role.ROLE_INSTITUTION_ADMIN;
        if (!canRevoke) {
            throw new AccessDeniedException("Only admins can revoke documents");
        }

        // Same institution check for INSTITUTION_ADMIN
        if (revoker.getRole() == Role.ROLE_INSTITUTION_ADMIN) {
            boolean sameInstitution = doc.getInstitution().getId()
                    .equals(revoker.getInstitution().getId());
            if (!sameInstitution) {
                throw new AccessDeniedException(
                        "You can only revoke documents from your own institution");
            }
        }

        doc.setStatus(DocumentStatus.REVOKED);
        doc.setRevocationReason(reason);
        documentRepository.save(doc);

        auditLogService.log(doc, AuditAction.REVOKED, revokerEmail, clientIp,
                "Revoked: " + reason);

        log.info("Document {} revoked by {}", documentId, revokerEmail);
        return documentService.toResponse(doc);
    }

    /**
     * Set an expiry date on an approved document.
     * Useful for temporary certificates or time-limited credentials.
     */
    @Transactional
    public DocumentResponse setExpiry(UUID documentId, LocalDateTime expiresAt,
                                       String requesterEmail, String clientIp) {
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));

        if (doc.getStatus() != DocumentStatus.APPROVED) {
            throw new InvalidStateTransitionException("Can only set expiry on APPROVED documents");
        }

        if (expiresAt != null && expiresAt.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Expiry date must be in the future");
        }

        User requester = userRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        boolean canSetExpiry = requester.getRole() == Role.ROLE_ADMIN
                || requester.getRole() == Role.ROLE_INSTITUTION_ADMIN
                || requester.getRole() == Role.ROLE_VERIFIER;
        if (!canSetExpiry) {
            throw new AccessDeniedException("Insufficient permissions to set expiry");
        }

        doc.setExpiresAt(expiresAt);
        documentRepository.save(doc);

        auditLogService.log(doc, AuditAction.APPROVED, requesterEmail, clientIp,
                expiresAt != null
                        ? "Expiry set to: " + expiresAt
                        : "Expiry removed");

        log.info("Expiry set for document {} → {}", documentId, expiresAt);
        return documentService.toResponse(doc);
    }

    /**
     * Scheduled job — runs every hour.
     * Finds APPROVED documents past their expiresAt and marks them REVOKED.
     */
    @Scheduled(fixedRate = 3_600_000) // every 1 hour
    @Transactional
    public void expireDocuments() {
        List<Document> expired = documentRepository
                .findByStatusAndExpiresAtBefore(DocumentStatus.APPROVED, LocalDateTime.now());

        if (expired.isEmpty()) return;

        log.info("Expiry job: found {} documents to expire", expired.size());

        for (Document doc : expired) {
            doc.setStatus(DocumentStatus.REVOKED);
            doc.setRevocationReason("Automatically expired on " + LocalDateTime.now());
            documentRepository.save(doc);
            auditLogService.log(doc, AuditAction.EXPIRED, "SYSTEM", "scheduler",
                    "Document expired automatically");
            log.info("Document {} auto-expired", doc.getId());
        }
    }
}