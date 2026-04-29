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

    // Paths that must be completely skipped — no JWT processing at all.
    // These are fully public and must never return 401 due to a bad/expired token.
    private static final String[] FULLY_EXCLUDED_PATHS = {
            "/auth/login",
            "/auth/signup",
            "/auth/refresh",
            "/auth/logout",
            "/public",
            "/v3/api-docs",
            "/swagger-ui",
            "/api/monitoring"
    };

    // Paths that are public (no auth required) but still benefit from having the
    // security context populated when a VALID token is present — e.g. OTP send/verify
    // can resolve the user from the JWT if available, but must NOT reject the request
    // when the token is absent or expired.
    private static final String[] OPTIONAL_AUTH_PATHS = {
            "/auth/otp/send",
            "/auth/otp/verify"
    };

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String requestURI = request.getRequestURI();
            log.debug("Incoming request: {}", requestURI);

            final String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                filterChain.doFilter(request, response);
                return;
            }

            String token = authHeader.substring(7);
            boolean isOptionalAuthPath = isOptionalAuthPath(requestURI);

            String username;
            try {
                username = authUtil.getUsernameFromToken(token);
            } catch (Exception e) {
                if (isOptionalAuthPath) {
                    // Public endpoint: an expired/invalid token is silently ignored.
                    // The OtpService will fall back to the username request param.
                    log.debug("[JWT] Expired/invalid token on public path {} — proceeding without auth context", requestURI);
                    filterChain.doFilter(request, response);
                    return;
                }
                // Protected endpoint: invalid token = 401.
                log.warn("[JWT] Invalid or expired JWT token on {}: {}", requestURI, e.getMessage());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"Invalid or expired token\", \"statusCode\": 401}");
                return;
            }

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                AppUser user = appUserRepository.findByUsername(username);
                if (user != null) {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }

            filterChain.doFilter(request, response);

        } catch (Exception ex) {
            handlerExceptionResolver.resolveException(request, response, null, ex);
        }
    }

    private boolean isOptionalAuthPath(String requestPath) {
        for (String path : OPTIONAL_AUTH_PATHS) {
            if (requestPath.startsWith(path)) return true;
        }
        return false;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        for (String excluded : FULLY_EXCLUDED_PATHS) {
            if (path.startsWith(excluded)) return true;
        }
        return false;
    }
}
