package com.docuverify.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class InstitutionResponse {
    private UUID id;
    private String name;
    private String domain;
    private String contactEmail;
    private boolean active;
    private long userCount;
    private long documentCount;
    private LocalDateTime createdAt;
}
