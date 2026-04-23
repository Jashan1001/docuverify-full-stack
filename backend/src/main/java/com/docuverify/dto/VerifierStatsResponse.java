package com.docuverify.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VerifierStatsResponse {
    private long queueSize;
    private long totalApproved;
    private long totalRejected;
    private long approvedToday;
    private long rejectedToday;
    private long urgentCount; // waiting > 48h
}
