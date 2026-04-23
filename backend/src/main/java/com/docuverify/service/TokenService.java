package com.docuverify.service;

import com.docuverify.entity.RefreshToken;
import com.docuverify.entity.User;
import com.docuverify.exception.TokenException;
import com.docuverify.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-token-expiry}")
    private long refreshTokenExpiry;

    @Transactional
    public RefreshToken createRefreshToken(User user) {
        // Revoke all existing active refresh tokens for this user (rotation policy)
        refreshTokenRepository.revokeAllUserTokens(user);

        RefreshToken refreshToken = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .expiryDate(LocalDateTime.now().plusSeconds(refreshTokenExpiry / 1000))
                .revoked(false)
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    @Transactional(readOnly = true)
    public RefreshToken verifyRefreshToken(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new TokenException("Refresh token not found"));

        if (refreshToken.isRevoked()) {
            throw new TokenException("Refresh token has been revoked");
        }

        if (refreshToken.isExpired()) {
            throw new TokenException("Refresh token has expired. Please login again.");
        }

        return refreshToken;
    }

    @Transactional
    public void revokeAllUserTokens(User user) {
        refreshTokenRepository.revokeAllUserTokens(user);
    }

    @Transactional
    public void deleteAllUserTokens(User user) {
        refreshTokenRepository.deleteAllByUser(user);
    }
}
