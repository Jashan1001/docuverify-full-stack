package com.docuverify.service;

import com.docuverify.dto.DocumentRequest;
import com.docuverify.dto.DocumentResponse;
import com.docuverify.entity.Document;
import com.docuverify.entity.Institution;
import com.docuverify.entity.User;
import com.docuverify.enums.DocumentStatus;
import com.docuverify.enums.Role;
import com.docuverify.exception.DuplicateResourceException;
import com.docuverify.repository.DocumentRepository;
import com.docuverify.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock DocumentRepository documentRepository;
    @Mock UserRepository userRepository;
    @Mock StorageService storageService;
    @Mock AuditLogService auditLogService;

    @InjectMocks DocumentService documentService;

    private User testUser;
    private Institution testInstitution;

    @BeforeEach
    void setUp() {
        testInstitution = Institution.builder()
                .id(UUID.randomUUID())
                .name("Test University")
                .domain("test.edu")
                .build();

        testUser = User.builder()
                .id(UUID.randomUUID())
                .email("user@test.edu")
                .fullName("Test User")
                .role(Role.ROLE_USER)
                .institution(testInstitution)
                .build();
    }

    @Test
    @DisplayName("Upload document — success")
    void uploadDocument_success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", "content".getBytes());

        DocumentRequest request = new DocumentRequest("My Certificate", "Description");

        when(userRepository.findByEmail("user@test.edu")).thenReturn(Optional.of(testUser));
        when(storageService.computeSha256(file)).thenReturn("abc123hash");
        when(documentRepository.existsByFileHash("abc123hash")).thenReturn(false);
        when(storageService.uploadFile(file, testInstitution.getId().toString()))
                .thenReturn("/api/files/" + testInstitution.getId() + "/test.pdf");
        when(documentRepository.save(any(Document.class))).thenAnswer(i -> i.getArgument(0));

        DocumentResponse response = documentService.uploadDocument(
                request, file, "user@test.edu", "127.0.0.1");

        assertThat(response.getTitle()).isEqualTo("My Certificate");
        assertThat(response.getStatus()).isEqualTo(DocumentStatus.UPLOADED);
        assertThat(response.getVerificationToken()).isNull();
        verify(auditLogService).log(any(), any(), eq("user@test.edu"), any(), any());
    }

    @Test
    @DisplayName("Upload document — duplicate file hash throws")
    void uploadDocument_duplicateHash_throws() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", "content".getBytes());

        when(userRepository.findByEmail("user@test.edu")).thenReturn(Optional.of(testUser));
        when(storageService.computeSha256(file)).thenReturn("duplicatehash");
        when(documentRepository.existsByFileHash("duplicatehash")).thenReturn(true);

        assertThatThrownBy(() ->
                documentService.uploadDocument(
                        new DocumentRequest("Title", null), file, "user@test.edu", "127.0.0.1"))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("Submit for review — success from UPLOADED state")
    void submitForReview_success() {
        Document doc = Document.builder()
                .id(UUID.randomUUID())
                .status(DocumentStatus.UPLOADED)
                .uploadedBy(testUser)
                .institution(testInstitution)
                .build();

        when(documentRepository.findById(doc.getId())).thenReturn(Optional.of(doc));
        when(userRepository.findByEmail("user@test.edu")).thenReturn(Optional.of(testUser));
        when(documentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        documentService.submitForReview(doc.getId(), "user@test.edu", "127.0.0.1");

        assertThat(doc.getStatus()).isEqualTo(DocumentStatus.UNDER_REVIEW);
    }

    @Test
    @DisplayName("Submit for review — throws if not owner")
    void submitForReview_notOwner_throws() {
        User otherUser = User.builder()
                .email("other@test.edu")
                .role(Role.ROLE_USER)
                .institution(testInstitution)
                .build();

        Document doc = Document.builder()
                .id(UUID.randomUUID())
                .status(DocumentStatus.UPLOADED)
                .uploadedBy(testUser)
                .institution(testInstitution)
                .build();

        when(documentRepository.findById(doc.getId())).thenReturn(Optional.of(doc));

        assertThatThrownBy(() ->
                documentService.submitForReview(doc.getId(), "other@test.edu", "127.0.0.1"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("Submit for review — throws if already under review")
    void submitForReview_wrongState_throws() {
        Document doc = Document.builder()
                .id(UUID.randomUUID())
                .status(DocumentStatus.UNDER_REVIEW)
                .uploadedBy(testUser)
                .institution(testInstitution)
                .build();

        when(documentRepository.findById(doc.getId())).thenReturn(Optional.of(doc));

        assertThatThrownBy(() ->
                documentService.submitForReview(doc.getId(), "user@test.edu", "127.0.0.1"))
                .isInstanceOf(IllegalStateException.class);
    }
}