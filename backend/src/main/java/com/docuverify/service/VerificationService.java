package com.docuverify.service;

import com.docuverify.dto.DocumentResponse;
import com.docuverify.dto.PublicVerificationResponse;
import com.docuverify.dto.VerificationRequest;
import com.docuverify.entity.Document;
import com.docuverify.entity.User;
import com.docuverify.enums.AuditAction;
import com.docuverify.enums.DocumentStatus;
import com.docuverify.exception.InvalidStateTransitionException;
import com.docuverify.exception.ResourceNotFoundException;
import com.docuverify.repository.DocumentRepository;
import com.docuverify.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class VerificationService {

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final DocumentService documentService;
    private final StorageService storageService;
    private final EmailService emailService;

    @Transactional
    public DocumentResponse approveDocument(VerificationRequest request,
                                             String verifierEmail, String clientIp) {
        Document doc = documentRepository.findById(request.getDocumentId())
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));

        if (doc.getStatus() != DocumentStatus.UNDER_REVIEW) {
            throw new InvalidStateTransitionException("Invalid document state for this operation");
        }

        User verifier = userRepository.findByEmail(verifierEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Verifier not found"));

        doc.setStatus(DocumentStatus.APPROVED);
        doc.setVerifiedBy(verifier);

        if (doc.getVerificationToken() == null) {
            doc.setVerificationToken(UUID.randomUUID().toString());
        }

        documentRepository.save(doc);

        auditLogService.log(doc, AuditAction.APPROVED, verifierEmail, clientIp,
                request.getRemarks() != null ? request.getRemarks() : "Document approved");

        emailService.sendApprovalEmail(
                doc.getUploadedBy().getEmail(),
                doc.getUploadedBy().getFullName(),
                doc.getTitle(),
                doc.getVerificationToken(),
                doc.getInstitution().getName()
        );

        log.info("Document {} approved by {}", doc.getId(), verifierEmail);
        return documentService.toResponse(doc);
    }

    @Transactional
    public DocumentResponse rejectDocument(VerificationRequest request,
                                            String verifierEmail, String clientIp) {
        Document doc = documentRepository.findById(request.getDocumentId())
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));

        if (doc.getStatus() != DocumentStatus.UNDER_REVIEW) {
            throw new InvalidStateTransitionException("Invalid document state for this operation");
        }

        if (request.getRejectionReason() == null || request.getRejectionReason().isBlank()) {
            throw new IllegalArgumentException("Rejection reason is required");
        }

        User verifier = userRepository.findByEmail(verifierEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Verifier not found"));

        doc.setStatus(DocumentStatus.REJECTED);
        doc.setVerifiedBy(verifier);
        doc.setRejectionReason(request.getRejectionReason());
        documentRepository.save(doc);

        auditLogService.log(doc, AuditAction.REJECTED, verifierEmail, clientIp,
                "Rejected: " + request.getRejectionReason());

        emailService.sendRejectionEmail(
                doc.getUploadedBy().getEmail(),
                doc.getUploadedBy().getFullName(),
                doc.getTitle(),
                doc.getRejectionReason(),
                doc.getInstitution().getName()
        );

        log.info("Document {} rejected by {}", doc.getId(), verifierEmail);
        return documentService.toResponse(doc);
    }

    @Transactional
    public PublicVerificationResponse verifyPublicly(String token, String clientIp) {
        Document doc = documentRepository.findByVerificationToken(token)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Document not found for this verification link"));

        auditLogService.log(doc, AuditAction.PUBLIC_VERIFIED, null, clientIp,
                "Public verification attempt");

        // ── Live tamper detection — works for both local and S3 ──────────────
        boolean tamperDetected = false;
        try {
            byte[] bytes = storageService.readFileBytes(doc.getFileUrl());
            String currentHash = storageService.computeSha256FromBytes(bytes);
            tamperDetected = !currentHash.equals(doc.getFileHash());
            if (tamperDetected) {
                log.warn("TAMPER DETECTED for document {}: hash mismatch", doc.getId());
            }
        } catch (Exception e) {
            log.warn("Integrity check failed for document {}: {}", doc.getId(), e.getMessage());
            tamperDetected = true;
        }
        // ─────────────────────────────────────────────────────────────────────

        boolean isApproved = doc.getStatus() == DocumentStatus.APPROVED;

        String message = switch (doc.getStatus()) {
            case APPROVED -> tamperDetected
                    ? "WARNING: This document may have been tampered with after approval."
                    : "Document is verified and authentic.";
            case REJECTED -> "Document has been rejected by the institution.";
            case UNDER_REVIEW -> "Document is currently under review.";
            case UPLOADED -> "Document has not been submitted for review yet.";
            case REVOKED -> "This document has been revoked by the institution.";
        };

        LocalDateTime verifiedAt = isApproved && doc.getUpdatedAt() != null
                ? doc.getUpdatedAt() : null;

        return PublicVerificationResponse.builder()
                .title(doc.getTitle())
                .status(doc.getStatus())
                .institutionName(doc.getInstitution().getName())
                .uploadedBy(doc.getUploadedBy().getFullName())
                .verifiedAt(verifiedAt)
                .tamperDetected(tamperDetected)
                .message(message)
                .build();
    }
}