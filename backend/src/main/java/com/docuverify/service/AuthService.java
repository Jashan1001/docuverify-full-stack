package com.docuverify.service;

import com.docuverify.dto.*;
import com.docuverify.entity.Institution;
import com.docuverify.entity.RefreshToken;
import com.docuverify.entity.User;
import com.docuverify.enums.Role;
import com.docuverify.exception.DuplicateResourceException;
import com.docuverify.exception.ResourceNotFoundException;
import com.docuverify.repository.InstitutionRepository;
import com.docuverify.repository.UserRepository;
import com.docuverify.security.CustomUserDetailsService;
import com.docuverify.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final InstitutionRepository institutionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final CustomUserDetailsService userDetailsService;

    private static final Set<String> PERSONAL_DOMAINS = Set.of(
            "gmail.com", "yahoo.com", "hotmail.com", "outlook.com",
            "icloud.com", "protonmail.com", "live.com", "aol.com",
            "ymail.com", "mail.com", "zoho.com", "rediffmail.com",
            "yandex.com", "gmx.com", "tutanota.com"
    );

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already registered: " + request.getEmail());
        }

        String email = request.getEmail().toLowerCase().trim();
        String emailDomain = email.substring(email.indexOf("@") + 1);

        Institution assignedInstitution;

        if (PERSONAL_DOMAINS.contains(emailDomain)) {
            assignedInstitution = institutionRepository
                    .findByName("Default Institution")
                    .orElse(null);
        } else {
            assignedInstitution = institutionRepository
                    .findByDomain(emailDomain)
                    .orElseGet(() -> institutionRepository.findByName("Default Institution").orElse(null));
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.ROLE_USER)
                .institution(assignedInstitution)
                .enabled(true)
                .build();

        userRepository.save(user);
        return buildTokenResponse(user);
    }

    @Transactional
    public AuthResponse login(AuthRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return buildTokenResponse(user);
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        RefreshToken existingToken = tokenService.verifyRefreshToken(request.getRefreshToken());
        User user = existingToken.getUser();
        tokenService.revokeAllUserTokens(user);
        RefreshToken newRefreshToken = tokenService.createRefreshToken(user);
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String newAccessToken = jwtUtil.generateAccessToken(userDetails);
        
        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken.getToken())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .institutionId(user.getInstitution() != null
                        ? user.getInstitution().getId().toString() : null)
                .build();
    }

    @Transactional
    public void logout(String email, String accessToken) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        tokenService.revokeAllUserTokens(user);
        // Redis-based blacklisting disabled
    }

    @Transactional
    public void verifyEmail(String token) {
        log.info("Verifying email with token: {}", token);
        // Implementation for email verification if needed
    }

    private AuthResponse buildTokenResponse(User user) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String accessToken = jwtUtil.generateAccessToken(userDetails);
        RefreshToken refreshToken = tokenService.createRefreshToken(user);
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .institutionId(user.getInstitution() != null
                        ? user.getInstitution().getId().toString() : null)
                .build();
    }
}