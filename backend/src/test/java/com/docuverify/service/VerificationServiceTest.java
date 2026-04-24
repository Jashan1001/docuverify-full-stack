package com.docuverify.service;

import com.docuverify.dto.DocumentResponse;
import com.docuverify.dto.VerificationRequest;
import com.docuverify.entity.Document;
import com.docuverify.entity.Institution;
import com.docuverify.entity.User;
import com.docuverify.enums.DocumentStatus;
import com.docuverify.enums.Role;
import com.docuverify.exception.InvalidStateTransitionException;
import com.docuverify.repository.DocumentRepository;
import com.docuverify.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VerificationServiceTest {

    @Mock DocumentRepository documentRepository;
    @Mock UserRepository userRepository;
    @Mock AuditLogService auditLogService;
    @Mock DocumentService documentService;
    @Mock StorageService storageService;
    @Mock EmailService emailService;

    @InjectMocks VerificationService verificationService;

    private User uploader;
    private User verifier;
    private Institution institution;
    private Document doc;

    @BeforeEach
    void setUp() {
        institution = Institution.builder()
                .id(UUID.randomUUID())
                .name("Test University")
                .build();

        uploader = User.builder()
                .id(UUID.randomUUID())
                .email("student@test.edu")
                .fullName("Student Name")
                .role(Role.ROLE_USER)
                .institution(institution)
                .build();

        verifier = User.builder()
                .id(UUID.randomUUID())
                .email("verifier@test.edu")
                .fullName("Verifier Name")
                .role(Role.ROLE_VERIFIER)
                .institution(institution)
                .build();

        doc = Document.builder()
                .id(UUID.randomUUID())
                .title("Degree Certificate")
                .status(DocumentStatus.UNDER_REVIEW)
                .uploadedBy(uploader)
                .institution(institution)
                .fileHash("abc123")
                .fileUrl("/api/files/" + institution.getId() + "/test.pdf")
                .build();
    }

    @Test
    @DisplayName("Approve document — assigns token and sets APPROVED")
    void approveDocument_success() {
        VerificationRequest request = new VerificationRequest();
        request.setDocumentId(doc.getId());
        request.setRemarks("Looks good");

        when(documentRepository.findById(doc.getId())).thenReturn(Optional.of(doc));
        when(userRepository.findByEmail("verifier@test.edu")).thenReturn(Optional.of(verifier));
        when(documentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(documentService.toResponse(any())).thenReturn(DocumentResponse.builder()
                .status(DocumentStatus.APPROVED).build());

        DocumentResponse response = verificationService.approveDocument(
                request, "verifier@test.edu", "127.0.0.1");

        assertThat(doc.getStatus()).isEqualTo(DocumentStatus.APPROVED);
        assertThat(doc.getVerificationToken()).isNotNull();
        assertThat(doc.getVerifiedBy()).isEqualTo(verifier);
        verify(emailService).sendApprovalEmail(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Approve document — throws if not UNDER_REVIEW")
    void approveDocument_wrongState_throws() {
        doc.setStatus(DocumentStatus.UPLOADED);
        VerificationRequest request = new VerificationRequest();
        request.setDocumentId(doc.getId());

        when(documentRepository.findById(doc.getId())).thenReturn(Optional.of(doc));

        assertThatThrownBy(() ->
                verificationService.approveDocument(request, "verifier@test.edu", "127.0.0.1"))
                .isInstanceOf(InvalidStateTransitionException.class);

        verify(emailService, never()).sendApprovalEmail(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Reject document — requires rejection reason")
    void rejectDocument_noReason_throws() {
        VerificationRequest request = new VerificationRequest();
        request.setDocumentId(doc.getId());
        request.setRejectionReason("");

        when(documentRepository.findById(doc.getId())).thenReturn(Optional.of(doc));

        assertThatThrownBy(() ->
                verificationService.rejectDocument(request, "verifier@test.edu", "127.0.0.1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Rejection reason is required");
    }

    @Test
    @DisplayName("Reject document — sets REJECTED and sends email")
    void rejectDocument_success() {
        VerificationRequest request = new VerificationRequest();
        request.setDocumentId(doc.getId());
        request.setRejectionReason("Document is not legible");

        when(documentRepository.findById(doc.getId())).thenReturn(Optional.of(doc));
        when(userRepository.findByEmail("verifier@test.edu")).thenReturn(Optional.of(verifier));
        when(documentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(documentService.toResponse(any())).thenReturn(DocumentResponse.builder()
                .status(DocumentStatus.REJECTED).build());

        verificationService.rejectDocument(request, "verifier@test.edu", "127.0.0.1");

        assertThat(doc.getStatus()).isEqualTo(DocumentStatus.REJECTED);
        assertThat(doc.getRejectionReason()).isEqualTo("Document is not legible");
        verify(emailService).sendRejectionEmail(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Approve twice — token not regenerated")
    void approveDocument_tokenNotRegenerated() {
        doc.setVerificationToken("existing-token-123");
        VerificationRequest request = new VerificationRequest();
        request.setDocumentId(doc.getId());

        when(documentRepository.findById(doc.getId())).thenReturn(Optional.of(doc));
        when(userRepository.findByEmail("verifier@test.edu")).thenReturn(Optional.of(verifier));
        when(documentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(documentService.toResponse(any())).thenReturn(DocumentResponse.builder().build());

        verificationService.approveDocument(request, "verifier@test.edu", "127.0.0.1");

        assertThat(doc.getVerificationToken()).isEqualTo("existing-token-123");
    }
}