package com.Job.Posting.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

@Component
@Slf4j
@RequiredArgsConstructor
public class RateLimiterFilter extends OncePerRequestFilter {

    // Limits for unauthenticated login or signup paths
    private static final int AUTH_MAX = 30;
    private static final Duration AUTH_WINDOW = Duration.ofMinutes(1);

    // Limits for every other endpoint authenticated
    private static final int GLOBAL_MAX = 200;
    private static final Duration GLOBAL_WINDOW = Duration.ofMinutes(1);

    private static final String KEY_PREFIX = "rate_limit:";

    private final StringRedisTemplate redisTemplate;

    private static final String[] EXCLUDED_PATHS = {
            "/actuator/health",
            "/actuator/info",
            "/api/monitoring"
    };

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        for (String excluded : EXCLUDED_PATHS) {
            if (path.startsWith(excluded)) return true;
        }
        return false;
    }

    @Override
    public void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException
    {
        String uri = request.getRequestURI();
        boolean isAuthEndpoint = uri.startsWith("/auth/");

        String bucketKey;
        int maxRequests;
        Duration window;

        if (isAuthEndpoint) {
            bucketKey   = KEY_PREFIX + "ip:" + request.getRemoteAddr();
            maxRequests = AUTH_MAX;
            window      = AUTH_WINDOW;
        } else {
            String principal = resolveUserId(request);
            bucketKey   = KEY_PREFIX + "user:" + principal;
            maxRequests = GLOBAL_MAX;
            window      = GLOBAL_WINDOW;
        }

        try {
            Long count = redisTemplate.opsForValue().increment(bucketKey);
            if (count != null && count == 1) {
                redisTemplate.expire(bucketKey, window);
            }
            if (count != null && count > maxRequests) {
                log.warn("Rate limit exceeded — key={} uri={}", bucketKey, uri);
                response.setStatus(429);
                response.setContentType("application/json");
                response.getWriter()
                        .write("{\"error\": \"Too many requests. Please try again later.\", \"statusCode\": 429}");
                return;
            }
        } catch (Exception e) {
            log.error("Rate limiter Redis error for key={}: {}", bucketKey, e.getMessage());
        }

        chain.doFilter(request, response);
    }

    private String resolveUserId(HttpServletRequest request) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                return auth.getName();
            }
        } catch (Exception ignored) {
        }
        return request.getRemoteAddr();
    }
}