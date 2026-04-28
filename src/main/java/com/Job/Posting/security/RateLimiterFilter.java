package com.Job.Posting.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

@Component
@Slf4j
@RequiredArgsConstructor
public class RateLimiterFilter extends OncePerRequestFilter {

    private static final int MAX_REQUESTS = 30;
    private static final Duration WINDOW = Duration.ofMinutes(1);
    private static final String KEY_PREFIX = "rate_limit:";

    // Shared across all pods — counts are stored in Redis, not per-instance memory
    private final StringRedisTemplate redisTemplate;

    private static final String[] EXCLUDED_PATHS = {
            "/public",
            "/actuator",
            "/v3/api-docs",
            "/swagger-ui",
            "/api/monitoring"
    };

    @Override
    public void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String uri = request.getRequestURI();

        //Only rate-limit auth endpoints
        if (!uri.startsWith("/auth/login") && !uri.startsWith("/auth/signup")) {
            filterChain.doFilter(request, response);
            return;
        }

        String ip = request.getRemoteAddr();
        String key = KEY_PREFIX + ip;

        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1) {
                //setting TTL on first request so the window resets automatically
                redisTemplate.expire(key, WINDOW);
            }
            if (count != null && count > MAX_REQUESTS) {
                log.warn("Rate limit exceeded for IP: {} on endpoint: {}", ip, uri);
                response.setStatus(429);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"Too many requests. Please try again later.\", \"statusCode\": 429}");
                return;
            }
        } catch (Exception e) {
            log.error("Rate limiter Redis error for IP {}: {}", ip, e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        for (String excluded : EXCLUDED_PATHS) {
            if (path.startsWith(excluded)) return true;
        }
        return false;
    }
}