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

@Service
@RequiredArgsConstructor
@Slf4j
public class VerificationService {

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final DocumentService documentService;

    /**
     * State machine: UNDER_REVIEW → APPROVED
     */
    @Transactional
    public DocumentResponse approveDocument(VerificationRequest request, String verifierEmail, String clientIp) {
        Document doc = documentRepository.findById(request.getDocumentId())
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));

        if (doc.getStatus() != DocumentStatus.UNDER_REVIEW) {
            throw new InvalidStateTransitionException("Invalid document state for this operation");
        }

        User verifier = userRepository.findByEmail(verifierEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Verifier not found"));

        doc.setStatus(DocumentStatus.APPROVED);
        doc.setVerifiedBy(verifier);
        documentRepository.save(doc);

        auditLogService.log(doc, AuditAction.APPROVED, verifierEmail, clientIp,
                request.getRemarks() != null ? request.getRemarks() : "Document approved");

        log.info("Document {} approved by {}", doc.getId(), verifierEmail);
        return documentService.toResponse(doc);
    }

    /**
     * State machine: UNDER_REVIEW → REJECTED
     */
    @Transactional
    public DocumentResponse rejectDocument(VerificationRequest request, String verifierEmail, String clientIp) {
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

        log.info("Document {} rejected by {}", doc.getId(), verifierEmail);
        return documentService.toResponse(doc);
    }

    /**
     * Public endpoint — no auth required.
     * Verifies document by its unique verificationToken (used in QR codes / share links).
     */
    @Transactional
    public PublicVerificationResponse verifyPublicly(String token, String clientIp) {
        Document doc = documentRepository.findByVerificationToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found for this verification link"));

        auditLogService.log(doc, AuditAction.PUBLIC_VERIFIED, null, clientIp,
                "Public verification attempt");

        boolean isApproved = doc.getStatus() == DocumentStatus.APPROVED;
        String message = switch (doc.getStatus()) {
            case APPROVED -> "Document is verified and authentic.";
            case REJECTED -> "Document has been rejected by the institution.";
            case UNDER_REVIEW -> "Document is currently under review.";
            case UPLOADED -> "Document has not been submitted for review yet.";
        };

        LocalDateTime verifiedAt = isApproved && doc.getUpdatedAt() != null ? doc.getUpdatedAt() : null;

        return PublicVerificationResponse.builder()
                .title(doc.getTitle())
                .status(doc.getStatus())
                .institutionName(doc.getInstitution().getName())
                .uploadedBy(doc.getUploadedBy().getFullName())
                .verifiedAt(verifiedAt)
                .tamperDetected(false)
                .message(message)
                .build();
    }
}
