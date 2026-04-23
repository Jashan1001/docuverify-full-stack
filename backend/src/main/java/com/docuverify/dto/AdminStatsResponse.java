package com.docuverify.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminStatsResponse {
    private long totalInstitutions;
    private long activeInstitutions;
    private long totalUsers;
    private long totalDocuments;
    private long approvedDocuments;
    private long pendingDocuments;
    private long verifiedToday;
}
