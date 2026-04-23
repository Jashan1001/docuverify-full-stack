package com.docuverify.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class VerificationRequest {

    @NotNull(message = "Document ID is required")
    private UUID documentId;

    @Size(max = 500, message = "Remarks cannot exceed 500 characters")
    private String remarks;

    // For rejection only
    @Size(max = 500, message = "Rejection reason cannot exceed 500 characters")
    private String rejectionReason;
}
