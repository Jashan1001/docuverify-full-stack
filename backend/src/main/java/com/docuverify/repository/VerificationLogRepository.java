package com.docuverify.repository;

import com.docuverify.entity.Document;
import com.docuverify.entity.VerificationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VerificationLogRepository extends JpaRepository<VerificationLog, UUID> {
    List<VerificationLog> findByDocumentOrderByTimestampDesc(Document document);
    Page<VerificationLog> findByDocument(Document document, Pageable pageable);
}
