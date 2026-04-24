package com.docuverify.enums;

public enum DocumentStatus {
    UPLOADED,
    UNDER_REVIEW,
    APPROVED,
    REJECTED,
    REVOKED   // new — admin/institution can revoke an approved document
}