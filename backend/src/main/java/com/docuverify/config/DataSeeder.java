package com.docuverify.config;

import com.docuverify.entity.Institution;
import com.docuverify.entity.User;
import com.docuverify.enums.Role;
import com.docuverify.repository.InstitutionRepository;
import com.docuverify.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final InstitutionRepository institutionRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${seeder.admin.email:admin@docuverify.com}")
    private String adminEmail;

    @Value("${seeder.admin.password:}")
    private String adminPassword;

    @Value("${seeder.admin.fullName:Platform Admin}")
    private String adminFullName;

    @Value("${seeder.institution.name:Default Institution}")
    private String institutionName;

    @Value("${seeder.institution.domain:docuverify.com}")
    private String institutionDomain;

    @Value("${seeder.institution.contact:admin@docuverify.com}")
    private String institutionContact;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedInstitution();
        seedAdmin();
    }

    private void seedInstitution() {
        if (!institutionRepository.existsByDomain(institutionDomain)) {
            Institution institution = Institution.builder()
                    .name(institutionName)
                    .domain(institutionDomain)
                    .contactEmail(institutionContact)
                    .active(true)
                    .build();
            institutionRepository.save(institution);
            log.info("✅ Seeded default institution: {}", institutionName);
        }
    }

    private void seedAdmin() {
        if (!userRepository.existsByEmail(adminEmail)) {
            if (adminPassword == null || adminPassword.isBlank() || adminPassword.length() < 12) {
                log.error("❌ Cannot seed platform admin: SEEDER_ADMIN_PASSWORD must be at least 12 characters!");
                throw new IllegalStateException("SEEDER_ADMIN_PASSWORD must be at least 12 characters");
            }
            if (adminEmail == null || !adminEmail.contains("@")) {
                throw new IllegalStateException("seeder.admin.email must be a valid email address");
            }
            if (institutionDomain == null || institutionDomain.isBlank()) {
                throw new IllegalStateException("seeder.institution.domain cannot be blank");
            }

            Institution institution = institutionRepository.findByDomain(institutionDomain)
                    .orElse(null);

            User admin = User.builder()
                    .fullName(adminFullName)
                    .email(adminEmail)
                    .password(passwordEncoder.encode(adminPassword))
                    .role(Role.ROLE_ADMIN)
                    .institution(institution)
                    .enabled(true)
                    .build();
            userRepository.save(admin);
            log.info("✅ Seeded platform admin: {}", adminEmail);
        }
    }
}
