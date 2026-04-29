package com.Job.Posting.security;

import com.Job.Posting.auth.repository.AppUserRepository;
import com.Job.Posting.entity.AppUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;

@Component
@Slf4j
@RequiredArgsConstructor
public class JWTAuthFilter extends OncePerRequestFilter {

    private final AuthUtil authUtil;
    private final AppUserRepository appUserRepository;
    private final HandlerExceptionResolver handlerExceptionResolver;

    // Paths that should skip JWT validation
    private static final String[] EXCLUDED_PATHS = {
            "/auth/login",
            "/auth/signup",
            "/auth/refresh",
            "/public",
            "/v3/api-docs",
            "/swagger-ui",
            "/api/monitoring"
    };

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        try {
            String requestURI = request.getRequestURI();
            log.info("Incoming Requests: {}", requestURI);

            final String requestAuthToken = request.getHeader("Authorization");
            if (requestAuthToken == null || !requestAuthToken.startsWith("Bearer ")) {
                filterChain.doFilter(request, response);
                return;
            }

            String token = requestAuthToken.split("Bearer ")[1];
            String username = null;

            try {
                username = authUtil.getUsernameFromToken(token);
            } catch (Exception e) {
                log.warn("Invalid or expired JWT token: {}", e.getMessage());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"Invalid or expired token\", \"statusCode\": 401}");
                return;
            }

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                AppUser user = appUserRepository.findByUsername(username);
                if (user == null) {
                    filterChain.doFilter(request, response);
                    return;
                }
                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            }

            filterChain.doFilter(request, response);
            return;

        } catch (Exception ex) {
            handlerExceptionResolver.resolveException(request, response, null, ex);
        }
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
}