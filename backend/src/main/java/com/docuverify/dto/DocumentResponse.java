package com.docuverify.dto;

import com.docuverify.enums.DocumentStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class DocumentResponse {
    private UUID id;
    private String title;
    private String description;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private String fileUrl;
    private DocumentStatus status;
    private String verificationToken;
    private String uploadedBy;
    private String institutionName;
    private String rejectionReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
