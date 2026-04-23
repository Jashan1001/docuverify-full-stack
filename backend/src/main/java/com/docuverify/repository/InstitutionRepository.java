package com.docuverify.repository;

import com.docuverify.entity.Institution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface InstitutionRepository extends JpaRepository<Institution, UUID> {
    Optional<Institution> findByDomain(String domain);
    Optional<Institution> findByName(String name);
    boolean existsByName(String name);
    boolean existsByDomain(String domain);
    long countByActive(boolean active);
}
