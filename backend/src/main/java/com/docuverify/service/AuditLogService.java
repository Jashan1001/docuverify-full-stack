package com.docuverify.service;

import com.docuverify.entity.Document;
import com.docuverify.entity.VerificationLog;
import com.docuverify.enums.AuditAction;
import com.docuverify.repository.VerificationLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final VerificationLogRepository logRepository;

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void log(Document document, AuditAction action, String performedBy, String ipAddress, String remarks) {
        VerificationLog entry = VerificationLog.builder()
                .document(document)
                .action(action)
                .performedBy(performedBy)
                .ipAddress(ipAddress)
                .remarks(remarks)
                .build();
        logRepository.save(entry);
    }

    @Transactional(readOnly = true)
    public List<com.docuverify.dto.VerificationLogResponse> getLogsForDocument(Document document) {
        return logRepository.findByDocumentOrderByTimestampDesc(document).stream()
                .map(log -> com.docuverify.dto.VerificationLogResponse.builder()
                        .id(log.getId())
                        .action(log.getAction().name())
                        .performedBy(log.getPerformedBy())
                        .ipAddress(log.getIpAddress())
                        .remarks(log.getRemarks())
                        .timestamp(log.getTimestamp())
                        .build())
                .collect(java.util.stream.Collectors.toList());
    }
}
