package com.docuverify.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class VerificationRequest {

    @NotNull(message = "Document ID is required")
    private UUID documentId;

    private String remarks;

    // For rejection only
    private String rejectionReason;
}
