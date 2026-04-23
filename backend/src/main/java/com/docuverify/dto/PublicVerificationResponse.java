package com.docuverify.dto;

import com.docuverify.enums.DocumentStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PublicVerificationResponse {
    private String title;
    private DocumentStatus status;
    private String institutionName;
    private String uploadedBy;
    private LocalDateTime verifiedAt;
    private boolean tamperDetected;
    private String message;
}
