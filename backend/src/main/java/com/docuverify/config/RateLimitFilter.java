package com.docuverify.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private final RedisTemplate<String, String> redisTemplate;

    @Value("${rate.limit.requests}")
    private int maxRequests;

    @Value("${rate.limit.window-seconds}")
    private int windowSeconds;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String key = buildKey(request);
        String countStr = redisTemplate.opsForValue().get(key);

        if (countStr == null) {
            redisTemplate.opsForValue().set(key, "1", Duration.ofSeconds(windowSeconds));
        } else {
            int count = Integer.parseInt(countStr);
            if (count >= maxRequests) {
                log.warn("Rate limit exceeded for key: {}", key);
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response.getWriter().write("""
                        {"error": "Rate limit exceeded. Try again later.", "status": 429}
                        """);
                return;
            }
            redisTemplate.opsForValue().increment(key);
        }

        filterChain.doFilter(request, response);
    }

    private String buildKey(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            return "rate_limit:user:" + auth.getName();
        }
        // Fall back to IP for public/unauthenticated endpoints
        return "rate_limit:ip:" + getClientIp(request);
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null && !xfHeader.isEmpty()) {
            return xfHeader.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
