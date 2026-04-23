package com.docuverify.service;

import com.docuverify.dto.DocumentRequest;
import com.docuverify.dto.DocumentResponse;
import com.docuverify.entity.Document;
import com.docuverify.entity.Institution;
import com.docuverify.entity.User;
import com.docuverify.enums.AuditAction;
import com.docuverify.enums.DocumentStatus;
import com.docuverify.enums.Role;
import com.docuverify.exception.DuplicateResourceException;
import com.docuverify.exception.ResourceNotFoundException;
import com.docuverify.repository.DocumentRepository;
import com.docuverify.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "image/png",
            "image/jpeg",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;
    private final AuditLogService auditLogService;

    @Transactional
    public DocumentResponse uploadDocument(
            DocumentRequest request,
            MultipartFile file,
            String uploaderEmail,
            String clientIp
    ) throws Exception {

        User uploader = userRepository.findByEmail(uploaderEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Institution institution = uploader.getInstitution();
        if (institution == null) {
            throw new IllegalStateException("User is not associated with any institution");
        }

        validateFileType(file);

        // Compute SHA-256 hash for tamper detection & deduplication
        String fileHash = storageService.computeSha256(file);
        if (documentRepository.existsByFileHash(fileHash)) {
            throw new DuplicateResourceException("This exact document has already been uploaded");
        }

        // Upload to S3
        String fileUrl = storageService.uploadFile(file, institution.getId().toString());

        Document document = Document.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .fileUrl(fileUrl)
                .fileName(file.getOriginalFilename())
                .fileType(file.getContentType())
                .fileSize(file.getSize())
                .fileHash(fileHash)
                .verificationToken(UUID.randomUUID().toString())
                .status(DocumentStatus.UPLOADED)
                .uploadedBy(uploader)
                .institution(institution)
                .build();

        documentRepository.save(document);

        auditLogService.log(document, AuditAction.UPLOADED, uploaderEmail, clientIp,
                "Document uploaded: " + file.getOriginalFilename());

        log.info("Document uploaded: {} by {}", document.getId(), uploaderEmail);
        return toResponse(document);
    }

    @Transactional(readOnly = true)
    public Page<DocumentResponse> getMyDocuments(String email, Pageable pageable) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return documentRepository.findByUploadedBy(user, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public DocumentResponse getDocumentById(UUID id, String requestorEmail) {
        Document doc = findDocumentOrThrow(id);
        User requestor = userRepository.findByEmail(requestorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Users can only see their own docs unless they're verifier/admin
        boolean isOwner = doc.getUploadedBy().getEmail().equals(requestorEmail);
        boolean isPrivileged = requestor.getRole() == Role.ROLE_VERIFIER
            || requestor.getRole() == Role.ROLE_ADMIN
            || requestor.getRole() == Role.ROLE_INSTITUTION_ADMIN;

        if (isPrivileged && requestor.getRole() != Role.ROLE_ADMIN) {
            if (requestor.getInstitution() == null || 
                !requestor.getInstitution().getId().equals(doc.getInstitution().getId())) {
                isPrivileged = false;
            }
        }

        if (!isOwner && !isPrivileged) {
            throw new AccessDeniedException("You don't have permission to view this document");
        }

        return toResponse(doc);
    }

    @Transactional
    public void submitForReview(UUID id, String requestorEmail, String clientIp) {
        Document doc = findDocumentOrThrow(id);

        if (!doc.getUploadedBy().getEmail().equals(requestorEmail)) {
            throw new AccessDeniedException("You can only submit your own documents");
        }

        if (doc.getStatus() != DocumentStatus.UPLOADED) {
            throw new IllegalStateException("Document is not in UPLOADED state");
        }

        doc.setStatus(DocumentStatus.UNDER_REVIEW);
        documentRepository.save(doc);

        auditLogService.log(doc, AuditAction.SUBMITTED_FOR_REVIEW, requestorEmail, clientIp,
                "Submitted for review");
        log.info("Document {} submitted for review by {}", id, requestorEmail);
    }

    @Transactional
    public void deleteDocument(UUID id, String requestorEmail) {
        Document doc = findDocumentOrThrow(id);

        if (!doc.getUploadedBy().getEmail().equals(requestorEmail)) {
            throw new AccessDeniedException("You can only delete your own documents");
        }

        if (doc.getStatus() == DocumentStatus.APPROVED) {
            throw new IllegalStateException("Approved documents cannot be deleted");
        }

        storageService.deleteFile(doc.getFileUrl());
        documentRepository.delete(doc);
        log.info("Document {} deleted by {}", id, requestorEmail);
    }

    @Transactional(readOnly = true)
    public Page<DocumentResponse> getPendingDocuments(String verifierEmail, Pageable pageable) {
        User verifier = userRepository.findByEmail(verifierEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Institution institution = verifier.getInstitution();
        if (institution == null) {
            return documentRepository.findByStatus(DocumentStatus.UNDER_REVIEW, pageable).map(this::toResponse);
        }
        return documentRepository.findByInstitutionAndStatus(institution, DocumentStatus.UNDER_REVIEW, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<DocumentResponse> getInstitutionDocuments(String requesterEmail, Pageable pageable, DocumentStatus status) {
        User requester = userRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        boolean canViewInstitutionDocs = requester.getRole() == Role.ROLE_VERIFIER
            || requester.getRole() == Role.ROLE_INSTITUTION_ADMIN
            || requester.getRole() == Role.ROLE_ADMIN;
        if (!canViewInstitutionDocs) {
            throw new AccessDeniedException("Access denied: insufficient permissions");
        }

        Institution institution = requester.getInstitution();
        if (institution == null) {
            if (requester.getRole() == Role.ROLE_ADMIN) {
                if (status == null) {
                    return documentRepository.findAll(pageable).map(this::toResponse);
                }
                return documentRepository.findByStatus(status, pageable).map(this::toResponse);
            }
            throw new AccessDeniedException("User is not associated with any institution");
        }

        if (status == null) {
            return documentRepository.findByInstitution(institution, pageable).map(this::toResponse);
        }
        return documentRepository.findByInstitutionAndStatus(institution, status, pageable).map(this::toResponse);
    }

    private Document findDocumentOrThrow(UUID id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + id));
    }

    public DocumentResponse toResponse(Document doc) {
        return DocumentResponse.builder()
                .id(doc.getId())
                .title(doc.getTitle())
                .description(doc.getDescription())
                .fileName(doc.getFileName())
                .fileType(doc.getFileType())
                .fileSize(doc.getFileSize())
                .fileUrl(doc.getFileUrl())
                .status(doc.getStatus())
                .verificationToken(doc.getVerificationToken())
                .uploadedBy(doc.getUploadedBy().getFullName())
                .institutionName(doc.getInstitution().getName())
                .rejectionReason(doc.getRejectionReason())
                .createdAt(doc.getCreatedAt())
                .updatedAt(doc.getUpdatedAt())
                .build();
    }

    public Document findByIdRaw(UUID id) {
        return findDocumentOrThrow(id);
    }

    private void validateFileType(MultipartFile file) throws java.io.IOException {
        org.apache.tika.Tika tika = new org.apache.tika.Tika();
        String detectedType = tika.detect(file.getInputStream());
        
        if (detectedType == null || !ALLOWED_CONTENT_TYPES.contains(detectedType)) {
            throw new IllegalArgumentException("Unsupported or spoofed file type. Detected: " + detectedType);
        }
    }
}
