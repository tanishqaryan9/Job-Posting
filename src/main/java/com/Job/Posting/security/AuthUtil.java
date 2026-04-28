package com.Job.Posting.security;

import com.Job.Posting.entity.AppUser;
import com.Job.Posting.entity.type.AuthProviderType;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
@Slf4j
public class AuthUtil {

    @Value("${jwt.secretKey}")
    private String jwtSecretKey;

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(jwtSecretKey.getBytes(StandardCharsets.UTF_8));
    }

    public String GenerateAccessToken(AppUser user) {
        Long profileId = user.getUserProfile() != null ? user.getUserProfile().getId() : null;

        return Jwts.builder()
                .claim("appUserId", user.getId())
                .claim("profileId", profileId)
                .subject(user.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 15))
                .signWith(getSecretKey())
                .compact();
    }

    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getSubject();
    }

    public AppUser getCurrentAppUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AppUser appUser)) {
            throw new RuntimeException("No authenticated user in security context");
        }
        return appUser;
    }

    public AuthProviderType getProviderTypeFromRegistrationID(String registrationid) {
        return switch (registrationid.toLowerCase()) {
            case "google"   -> AuthProviderType.GOOGLE;
            case "github"   -> AuthProviderType.GITHUB;
            case "facebook" -> AuthProviderType.FACEBOOK;
            case "twitter"  -> AuthProviderType.TWITTER;
            default -> throw new IllegalArgumentException("Unsupported OAuth2 Provider: " + registrationid);
        };
    }

    public String determineProviderIDFromOAuth2User(OAuth2User user, String registrationid) {
        String providerId = switch (registrationid.toLowerCase()) {
            case "google" -> user.getAttribute("sub");
            case "github" -> user.getAttribute("id").toString();
            default -> {
                log.error("Unsupported OAuth2 Provider: {}", registrationid);
                throw new IllegalArgumentException("Unsupported OAuth2 Provider: " + registrationid);
            }
        };
        if (providerId == null || providerId.isBlank()) {
            log.error("Unable to determine providerID for Provider: {}", registrationid);
            throw new IllegalArgumentException("Unable to determine providerID for OAuth2 login");
        }
        return providerId;
    }

    public String determineDisplayNameFromOAuth2User(OAuth2User user, String registrationId) {
        String name = user.getAttribute("name");
        if (name != null && !name.isBlank()) return name;
        if ("github".equalsIgnoreCase(registrationId)) {
            String login = user.getAttribute("login");
            if (login != null && !login.isBlank()) return login;
        }
        return null;
    }

    public String determineUsernameFromOAuth2User(OAuth2User user, String registrationId, String providerId) {
        String email = user.getAttribute("email");
        if (email != null && !email.isBlank()) return email;
        return switch (registrationId.toLowerCase()) {
            case "google" -> user.getAttribute("sub");
            case "github" -> user.getAttribute("login");
            default -> providerId;
        };
    }
}