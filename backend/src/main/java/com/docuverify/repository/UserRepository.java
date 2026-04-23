package com.docuverify.repository;

import com.docuverify.entity.Institution;
import com.docuverify.entity.User;
import com.docuverify.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    long countByRole(Role role);
    long countByInstitution(Institution institution);
    long countByInstitutionAndRole(Institution institution, Role role);
    Page<User> findByInstitution(Institution institution, Pageable pageable);
    Page<User> findByRole(Role role, Pageable pageable);
    List<User> findByInstitutionAndRole(Institution institution, Role role);
    boolean existsByRole(Role role);

    @Query("SELECT COUNT(u) FROM User u WHERE u.institution = :institution AND u.role IN ('ROLE_VERIFIER', 'ROLE_INSTITUTION_ADMIN')")
    long countStaffByInstitution(Institution institution);

    @Query("SELECT u.institution.id, COUNT(u) FROM User u WHERE u.institution.id IN :institutionIds GROUP BY u.institution.id")
    List<Object[]> countUsersByInstitutionIds(@org.springframework.data.repository.query.Param("institutionIds") List<UUID> institutionIds);
}
