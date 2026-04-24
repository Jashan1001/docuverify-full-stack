package com.docuverify.service;

import com.docuverify.dto.AuthResponse;
import com.docuverify.dto.RegisterRequest;
import com.docuverify.entity.Institution;
import com.docuverify.entity.RefreshToken;
import com.docuverify.entity.User;
import com.docuverify.enums.Role;
import com.docuverify.exception.DuplicateResourceException;
import com.docuverify.repository.InstitutionRepository;
import com.docuverify.repository.UserRepository;
import com.docuverify.security.CustomUserDetailsService;
import com.docuverify.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock InstitutionRepository institutionRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtUtil jwtUtil;
    @Mock AuthenticationManager authenticationManager;
    @Mock TokenService tokenService;
    @Mock CustomUserDetailsService userDetailsService;
    @Mock AccessTokenBlocklistService accessTokenBlocklistService;

    @InjectMocks AuthService authService;

    private Institution defaultInstitution;
    private Institution universityInstitution;

    @BeforeEach
    void setUp() {
        defaultInstitution = Institution.builder()
                .id(UUID.randomUUID())
                .name("Default Institution")
                .domain("docuverify.com")
                .build();

        universityInstitution = Institution.builder()
                .id(UUID.randomUUID())
                .name("Chandigarh University")
                .domain("cu.ac.in")
                .build();
    }

    @Test
    @DisplayName("Register — institutional email assigns correct institution")
    void register_institutionalEmail_assignsInstitution() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Test Student");
        request.setEmail("student@cu.ac.in");
        request.setPassword("password123");

        when(userRepository.existsByEmail("student@cu.ac.in")).thenReturn(false);
        when(institutionRepository.findByDomain("cu.ac.in"))
                .thenReturn(Optional.of(universityInstitution));
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(userDetailsService.loadUserByUsername(anyString()))
                .thenReturn(mock(org.springframework.security.core.userdetails.UserDetails.class));
        when(jwtUtil.generateAccessToken(any())).thenReturn("access-token");
        when(tokenService.createRefreshToken(any())).thenReturn(
                RefreshToken.builder().token("refresh-token")
                        .user(User.builder().build()).build());

        authService.register(request);

        verify(userRepository).save(argThat(user ->
                user.getInstitution().equals(universityInstitution)
                && user.getRole() == Role.ROLE_USER
        ));
    }

    @Test
    @DisplayName("Register — Gmail falls to Default Institution")
    void register_gmailEmail_fallsToDefault() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Test User");
        request.setEmail("user@gmail.com");
        request.setPassword("password123");

        when(userRepository.existsByEmail("user@gmail.com")).thenReturn(false);
        when(institutionRepository.findByName("Default Institution"))
                .thenReturn(Optional.of(defaultInstitution));
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(userDetailsService.loadUserByUsername(anyString()))
                .thenReturn(mock(org.springframework.security.core.userdetails.UserDetails.class));
        when(jwtUtil.generateAccessToken(any())).thenReturn("access-token");
        when(tokenService.createRefreshToken(any())).thenReturn(
                RefreshToken.builder().token("refresh-token")
                        .user(User.builder().build()).build());

        authService.register(request);

        // Should NOT try domain lookup for gmail
        verify(institutionRepository, never()).findByDomain("gmail.com");
        verify(userRepository).save(argThat(user ->
                user.getInstitution().equals(defaultInstitution)));
    }

    @Test
    @DisplayName("Register — duplicate email throws")
    void register_duplicateEmail_throws() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("existing@test.edu");
        request.setPassword("password123");
        request.setFullName("Test");

        when(userRepository.existsByEmail("existing@test.edu")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("Register — always assigns ROLE_USER regardless of input")
    void register_alwaysAssignsRoleUser() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Attacker");
        request.setEmail("attacker@cu.ac.in");
        request.setPassword("password123");

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(institutionRepository.findByDomain("cu.ac.in"))
                .thenReturn(Optional.of(universityInstitution));
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(userDetailsService.loadUserByUsername(anyString()))
                .thenReturn(mock(org.springframework.security.core.userdetails.UserDetails.class));
        when(jwtUtil.generateAccessToken(any())).thenReturn("token");
        when(tokenService.createRefreshToken(any())).thenReturn(
                RefreshToken.builder().token("rt")
                        .user(User.builder().build()).build());

        authService.register(request);

        verify(userRepository).save(argThat(user ->
                user.getRole() == Role.ROLE_USER));
    }
}