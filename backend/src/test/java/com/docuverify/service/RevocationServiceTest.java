package com.docuverify.service;

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
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RevocationServiceTest {

    @Mock DocumentRepository documentRepository;
    @Mock UserRepository userRepository;
    @Mock AuditLogService auditLogService;
    @Mock DocumentService documentService;

    @InjectMocks RevocationService revocationService;

    private User admin;
    private User regularUser;
    private Institution institution;
    private Document doc;

    @BeforeEach
    void setUp() {
        institution = Institution.builder()
                .id(UUID.randomUUID())
                .name("Test University")
                .build();

        admin = User.builder()
                .id(UUID.randomUUID())
                .email("admin@docuverify.com")
                .role(Role.ROLE_ADMIN)
                .institution(institution)
                .build();

        regularUser = User.builder()
                .id(UUID.randomUUID())
                .email("user@test.edu")
                .role(Role.ROLE_USER)
                .institution(institution)
                .build();

        doc = Document.builder()
                .id(UUID.randomUUID())
                .title("Test Certificate")
                .status(DocumentStatus.APPROVED)
                .uploadedBy(regularUser)
                .institution(institution)
                .verificationToken("some-token")
                .build();
    }

    @Test
    @DisplayName("Revoke document — admin success")
    void revokeDocument_adminSuccess() {
        when(documentRepository.findById(doc.getId())).thenReturn(Optional.of(doc));
        when(userRepository.findByEmail("admin@docuverify.com")).thenReturn(Optional.of(admin));
        when(documentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(documentService.toResponse(any())).thenReturn(null);

        revocationService.revokeDocument(doc.getId(), "Issued in error",
                "admin@docuverify.com", "127.0.0.1");

        assertThat(doc.getStatus()).isEqualTo(DocumentStatus.REVOKED);
        assertThat(doc.getRevocationReason()).isEqualTo("Issued in error");
    }

    @Test
    @DisplayName("Revoke document — regular user throws AccessDeniedException")
    void revokeDocument_regularUser_throws() {
        when(documentRepository.findById(doc.getId())).thenReturn(Optional.of(doc));
        when(userRepository.findByEmail("user@test.edu")).thenReturn(Optional.of(regularUser));

        assertThatThrownBy(() ->
                revocationService.revokeDocument(doc.getId(), "reason",
                        "user@test.edu", "127.0.0.1"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("Revoke document — throws if not APPROVED")
    void revokeDocument_wrongState_throws() {
        doc.setStatus(DocumentStatus.UNDER_REVIEW);
        when(documentRepository.findById(doc.getId())).thenReturn(Optional.of(doc));

        assertThatThrownBy(() ->
                revocationService.revokeDocument(doc.getId(), "reason",
                        "admin@docuverify.com", "127.0.0.1"))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    @DisplayName("Set expiry — past date throws")
    void setExpiry_pastDate_throws() {
        when(documentRepository.findById(doc.getId())).thenReturn(Optional.of(doc));
        when(userRepository.findByEmail("admin@docuverify.com")).thenReturn(Optional.of(admin));

        LocalDateTime pastDate = LocalDateTime.now().minusDays(1);

        assertThatThrownBy(() ->
                revocationService.setExpiry(doc.getId(), pastDate,
                        "admin@docuverify.com", "127.0.0.1"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Expiry scheduler — expires overdue documents")
    void expireDocuments_expiresOverdue() {
        doc.setExpiresAt(LocalDateTime.now().minusHours(1));
        when(documentRepository.findByStatusAndExpiresAtBefore(
                eq(DocumentStatus.APPROVED), any(LocalDateTime.class)))
                .thenReturn(java.util.List.of(doc));
        when(documentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        revocationService.expireDocuments();

        assertThat(doc.getStatus()).isEqualTo(DocumentStatus.REVOKED);
        verify(auditLogService).log(any(), any(), eq("SYSTEM"), eq("scheduler"), any());
    }
}