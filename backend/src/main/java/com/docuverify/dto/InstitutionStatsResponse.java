package com.docuverify.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InstitutionStatsResponse {
    private long totalDocuments;
    private long approvedDocuments;
    private long pendingDocuments;
    private long rejectedDocuments;
    private long totalMembers;
    private long verifierCount;
    private String institutionName;
}
