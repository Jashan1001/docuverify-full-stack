package com.docuverify.service;

import com.docuverify.dto.DocumentRequest;
import com.docuverify.entity.Institution;
import com.docuverify.entity.User;
import com.docuverify.repository.DocumentRepository;
import com.docuverify.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private StorageService storageService;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private org.apache.tika.Tika tika;

    @InjectMocks
    private DocumentService documentService;

    private User uploader;
    private Institution institution;

    @BeforeEach
    void setUp() {
        institution = new Institution();
        institution.setId(UUID.randomUUID());
        institution.setName("Test Institution");

        uploader = new User();
        uploader.setEmail("uploader@test.com");
        uploader.setInstitution(institution);
    }

    @Test
    void uploadDocument_success() throws Exception {
        DocumentRequest request = new DocumentRequest();
        request.setTitle("Test Doc");

        MockMultipartFile file = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", "dummy content".getBytes()
        );

        when(userRepository.findByEmail("uploader@test.com")).thenReturn(Optional.of(uploader));
        when(storageService.computeSha256(any(org.springframework.web.multipart.MultipartFile.class))).thenReturn("dummyHash");
        when(documentRepository.existsByFileHashAndStatusNot("dummyHash", com.docuverify.enums.DocumentStatus.REJECTED)).thenReturn(false);
        when(storageService.uploadFile(any(), anyString())).thenReturn("/api/files/test.pdf");

        when(tika.detect(any(byte[].class))).thenReturn("application/pdf");

        documentService.uploadDocument(request, file, "uploader@test.com", "127.0.0.1");

        verify(documentRepository).save(any(com.docuverify.entity.Document.class));
        verify(auditLogService).log(any(), any(), eq("uploader@test.com"), eq("127.0.0.1"), anyString());
    }

    @Test
    void uploadDocument_userNotFound() {
        DocumentRequest request = new DocumentRequest();
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", "dummy".getBytes());

        when(userRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

        assertThrows(com.docuverify.exception.ResourceNotFoundException.class, () -> {
            documentService.uploadDocument(request, file, "unknown@test.com", "127.0.0.1");
        });
    }

    @Test
    void getDocumentById_accessDenied() {
        com.docuverify.entity.Document doc = new com.docuverify.entity.Document();
        doc.setId(UUID.randomUUID());
        doc.setUploadedBy(uploader);
        
        Institution otherInst = new Institution();
        otherInst.setId(UUID.randomUUID());
        doc.setInstitution(otherInst);

        User requestor = new User();
        requestor.setEmail("other@test.com");
        requestor.setRole(com.docuverify.enums.Role.ROLE_VERIFIER);
        requestor.setInstitution(institution); // Different institution

        when(documentRepository.findById(doc.getId())).thenReturn(Optional.of(doc));
        when(userRepository.findByEmail("other@test.com")).thenReturn(Optional.of(requestor));

        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            documentService.getDocumentById(doc.getId(), "other@test.com");
        });
    }

    @Test
    void uploadDocument_duplicateHash() throws Exception {
        DocumentRequest request = new DocumentRequest();
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", "%PDF-1.4 dummy".getBytes());

        when(userRepository.findByEmail("uploader@test.com")).thenReturn(Optional.of(uploader));
        when(tika.detect(any(byte[].class))).thenReturn("application/pdf");
        when(storageService.computeSha256(any(org.springframework.web.multipart.MultipartFile.class))).thenReturn("dupHash");
        when(documentRepository.existsByFileHashAndStatusNot("dupHash", com.docuverify.enums.DocumentStatus.REJECTED)).thenReturn(true);

        assertThrows(com.docuverify.exception.DuplicateResourceException.class, () -> {
            documentService.uploadDocument(request, file, "uploader@test.com", "127.0.0.1");
        });
    }

    @Test
    void submitForReview_wrongOwner() {
        com.docuverify.entity.Document doc = new com.docuverify.entity.Document();
        doc.setId(UUID.randomUUID());
        doc.setUploadedBy(uploader);

        when(documentRepository.findById(doc.getId())).thenReturn(Optional.of(doc));

        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            documentService.submitForReview(doc.getId(), "wrong@test.com", "127.0.0.1");
        });
    }

    @Test
    void submitForReview_wrongStatus() {
        com.docuverify.entity.Document doc = new com.docuverify.entity.Document();
        doc.setId(UUID.randomUUID());
        doc.setUploadedBy(uploader);
        doc.setStatus(com.docuverify.enums.DocumentStatus.UNDER_REVIEW);

        when(documentRepository.findById(doc.getId())).thenReturn(Optional.of(doc));

        assertThrows(IllegalStateException.class, () -> {
            documentService.submitForReview(doc.getId(), "uploader@test.com", "127.0.0.1");
        });
    }

    @Test
    void deleteDocument_approvedDoc() {
        com.docuverify.entity.Document doc = new com.docuverify.entity.Document();
        doc.setId(UUID.randomUUID());
        doc.setUploadedBy(uploader);
        doc.setStatus(com.docuverify.enums.DocumentStatus.APPROVED);

        when(documentRepository.findById(doc.getId())).thenReturn(Optional.of(doc));

        assertThrows(IllegalStateException.class, () -> {
            documentService.deleteDocument(doc.getId(), "uploader@test.com");
        });
    }
}
