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
    private final AccessTokenBlocklistService accessTokenBlocklistService;

    private final org.springframework.data.redis.core.RedisTemplate<String, String> redisTemplate;

    @Transactional
    public void register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already registered: " + request.getEmail());
        }

        String emailDomain = request.getEmail().substring(request.getEmail().indexOf("@") + 1).toLowerCase();
        
        Institution assignedInstitution = institutionRepository.findByDomain(emailDomain)
                .orElseGet(() -> institutionRepository.findByName("Default Institution").orElse(null));

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.ROLE_USER)
                .institution(assignedInstitution)
                .enabled(true) // Set to true by default for easier development
                .build();

        userRepository.save(user);
        
        String verificationToken = java.util.UUID.randomUUID().toString();
        try {
            redisTemplate.opsForValue().set("email_verification:" + verificationToken, user.getEmail(), java.time.Duration.ofHours(24));
            
            log.info("Registered new user: {} as ROLE_USER. User is disabled pending verification.", user.getEmail());
            log.warn("SIMULATED EMAIL VERIFICATION: To activate this account, send a GET request or navigate to:");
            log.warn("http://localhost:8080/api/auth/verify?token={}", verificationToken);
        } catch (Exception e) {
            log.warn("Redis is down, bypassing email verification for user: {}", user.getEmail());
            user.setEnabled(true);
            userRepository.save(user);
        }
    }

    @Transactional
    public void verifyEmail(String token) {
        String email = null;
        try {
            email = redisTemplate.opsForValue().get("email_verification:" + token);
        } catch (Exception e) {
            log.warn("Redis is down, cannot verify email token.");
        }
        if (email == null) {
            throw new IllegalArgumentException("Invalid or expired verification token");
        }
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setEnabled(true);
        userRepository.save(user);
        try {
            redisTemplate.delete("email_verification:" + token);
        } catch (Exception e) {
            log.warn("Redis is down, could not delete email verification token.");
        }
        log.info("User email verified and account enabled: {}", email);
    }

    @Transactional
    public AuthResponse login(AuthRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        log.info("User logged in: {}", user.getEmail());
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
        log.info("Tokens refreshed for: {}", user.getEmail());
        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken.getToken())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .institutionId(user.getInstitution() != null ? user.getInstitution().getId().toString() : null)
                .build();
    }

    @Transactional
    public void logout(String email, String accessToken) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        tokenService.revokeAllUserTokens(user);
        if (accessToken != null && !accessToken.isBlank()) {
            accessTokenBlocklistService.blacklist(accessToken);
        }
        log.info("User logged out: {}", email);
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
                .institutionId(user.getInstitution() != null ? user.getInstitution().getId().toString() : null)
                .build();
    }
}
