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

    @Value("${seeder.admin.password:ChangeMe@123}")
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
            log.info("⚠️  Change the admin password immediately in production!");
        }
    }
}
