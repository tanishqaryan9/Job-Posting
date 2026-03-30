package com.Job.Posting.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Slf4j
public class RateLimiterFilter extends OncePerRequestFilter {

    //Max 10 requests per minute per IP on auth endpoints
    private static final int MAX_REQUESTS = 10;
    private static final long WINDOWS_MS = 60_000;

    private final Map<String, AtomicInteger> requestCount = new ConcurrentHashMap<>();
    private final Map<String, Long> windowsStart = new ConcurrentHashMap<>();

    // Paths that should skip rate limiting
    private static final String[] EXCLUDED_PATHS = {
            "/public",
            "/actuator",
            "/v3/api-docs",
            "/swagger-ui",
            "/api/monitoring"  // ADD THIS
    };

    @Override
    public void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String uri = request.getRequestURI();

        // Skip rate limiting for excluded paths
        if (shouldExcludePath(uri)) {
            filterChain.doFilter(request, response);
            return;
        }

        //only limiting auth endpoints
        if (!uri.startsWith("/auth/login") && !uri.startsWith("/auth/signup")) {
            filterChain.doFilter(request, response);
            return;
        }

        String ip = getClientIp(request);
        long now = System.currentTimeMillis();

        windowsStart.putIfAbsent(ip, now);
        requestCount.putIfAbsent(ip, new AtomicInteger(0));

        //reseting window if expired
        if (now - windowsStart.get(ip) > WINDOWS_MS) {
            windowsStart.put(ip, now);
            requestCount.get(ip).set(0);
        }

        int count = requestCount.get(ip).incrementAndGet();
        if (count > MAX_REQUESTS) {
            log.warn("Rate limiting exceeded for IP: {} on endpoint: {}", ip, uri);
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Too many requests, PLease try again later.\", \"statusCode\": 429}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean shouldExcludePath(String requestPath) {
        for (String excludedPath : EXCLUDED_PATHS) {
            if (requestPath.startsWith(excludedPath)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        return shouldExcludePath(path);
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}