package com.docuverify.enums;

public enum AuditAction {
    UPLOADED,
    SUBMITTED_FOR_REVIEW,
    APPROVED,
    REJECTED,
    REVOKED,        // new
    PUBLIC_VERIFIED,
    VIEWED,
    DELETED,
    EXPIRED         // new — set by scheduler
}