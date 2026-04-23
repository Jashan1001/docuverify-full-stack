package com.docuverify.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class VerificationLogResponse {
    private UUID id;
    private String action;
    private String performedBy;
    private String ipAddress;
    private String remarks;
    private LocalDateTime timestamp;
}
