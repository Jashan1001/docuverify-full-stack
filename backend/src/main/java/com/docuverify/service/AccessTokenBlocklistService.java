package com.docuverify.service;

import com.docuverify.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccessTokenBlocklistService {

    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";

    private final RedisTemplate<String, String> redisTemplate;
    private final JwtUtil jwtUtil;

    public void blacklist(String token) {
        Date expiration = jwtUtil.extractExpiration(token);
        long seconds = Duration.between(Instant.now(), expiration.toInstant()).getSeconds();
        if (seconds <= 0) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(BLACKLIST_PREFIX + token, "1", Duration.ofSeconds(seconds));
            log.info("Access token blacklisted for {} seconds", seconds);
        } catch (Exception e) {
            log.warn("Failed to blacklist token due to Redis error: {}", e.getMessage());
        }
    }

    public boolean isBlacklisted(String token) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + token));
        } catch (Exception e) {
            log.warn("Failed to check token blacklist due to Redis error: {}", e.getMessage());
            return false;
        }
    }
}