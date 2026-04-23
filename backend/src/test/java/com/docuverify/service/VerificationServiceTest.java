package com.docuverify.service;

import com.docuverify.dto.DocumentResponse;
import com.docuverify.dto.PublicVerificationResponse;
import com.docuverify.dto.VerificationRequest;
import com.docuverify.entity.Document;
import com.docuverify.entity.Institution;
import com.docuverify.entity.User;
import com.docuverify.enums.DocumentStatus;
import com.docuverify.exception.InvalidStateTransitionException;
import com.docuverify.repository.DocumentRepository;
import com.docuverify.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VerificationServiceTest {

    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private DocumentService documentService;
    @Mock
    private StorageService storageService;

    @InjectMocks
    private VerificationService verificationService;

    private Document doc;
    private User verifier;

    @BeforeEach
    void setUp() {
        Institution institution = new Institution();
        institution.setId(UUID.randomUUID());
        institution.setName("Test Institution");

        User uploader = new User();
        uploader.setFullName("Uploader Name");

        doc = new Document();
        doc.setId(UUID.randomUUID());
        doc.setStatus(DocumentStatus.UNDER_REVIEW);
        doc.setInstitution(institution);
        doc.setUploadedBy(uploader);
        doc.setFileUrl("/api/files/test.pdf");
        doc.setFileHash("correctHash");
        doc.setTitle("Test Doc");

        verifier = new User();
        verifier.setEmail("verifier@test.com");
    }

    @Test
    void approveDocument_success() {
        VerificationRequest request = new VerificationRequest();
        request.setDocumentId(doc.getId());
        request.setRemarks("Looks good");

        when(documentRepository.findById(doc.getId())).thenReturn(Optional.of(doc));
        when(userRepository.findByEmail("verifier@test.com")).thenReturn(Optional.of(verifier));
        when(documentService.toResponse(any())).thenReturn(DocumentResponse.builder().build());

        verificationService.approveDocument(request, "verifier@test.com", "127.0.0.1");

        assertEquals(DocumentStatus.APPROVED, doc.getStatus());
        assertEquals(verifier, doc.getVerifiedBy());
        verify(documentRepository).save(doc);
    }

    @Test
    void approveDocument_wrongState() {
        doc.setStatus(DocumentStatus.UPLOADED);
        VerificationRequest request = new VerificationRequest();
        request.setDocumentId(doc.getId());

        when(documentRepository.findById(doc.getId())).thenReturn(Optional.of(doc));

        assertThrows(InvalidStateTransitionException.class, () -> {
            verificationService.approveDocument(request, "verifier@test.com", "127.0.0.1");
        });
    }

    @Test
    void rejectDocument_missingReason() {
        VerificationRequest request = new VerificationRequest();
        request.setDocumentId(doc.getId());

        when(documentRepository.findById(doc.getId())).thenReturn(Optional.of(doc));

        assertThrows(IllegalArgumentException.class, () -> {
            verificationService.rejectDocument(request, "verifier@test.com", "127.0.0.1");
        });
    }

    @Test
    void verifyPublicly_tamperDetected() throws Exception {
        when(documentRepository.findByVerificationToken("token123")).thenReturn(Optional.of(doc));
        when(storageService.resolveFilePath(anyString(), anyString())).thenReturn(java.nio.file.Path.of("test.pdf"));
        when(storageService.computeSha256(any(java.nio.file.Path.class))).thenReturn("wrongHash");

        PublicVerificationResponse response = verificationService.verifyPublicly("token123", "127.0.0.1");

        assertTrue(response.isTamperDetected());
    }
}
