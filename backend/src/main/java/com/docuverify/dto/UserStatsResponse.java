package com.docuverify.dto;

import lombok.Builder;
import lombok.Data;

// User dashboard stats
@Data
@Builder
public class UserStatsResponse {
    private long total;
    private long approved;
    private long underReview;
    private long rejected;
    private long uploaded;
}
