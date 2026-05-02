package com.Job.Posting.otp.service;

import com.Job.Posting.auth.repository.AppUserRepository;
import com.Job.Posting.entity.AppUser;
import com.Job.Posting.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.security.SecureRandom;
import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private static final String KEY_PREFIX = "otp:";
    private static final SecureRandom RANDOM = new SecureRandom();

    @Value("${otp.expiry.minutes:10}")
    private long otpExpiryMinutes;

    @Value("${MAIL_FROM:${spring.mail.username:}}")
    private String mailFrom;

    private final StringRedisTemplate redisTemplate;
    private final EmailService emailService;
    private final AppUserRepository appUserRepository;
    private final com.Job.Posting.user.repository.UserRepository userRepository;

    // ── Public API ────────────────────────────────────────────────────────────

    public void sendOtp(String type, String value) {
        sendOtp(type, value, null);
    }

    public void sendOtp(String type, String value, String username) {
        String normalizedType = type != null ? type.trim().toUpperCase() : "";
        if (!"EMAIL".equals(normalizedType) && !"FORGOT_PASSWORD".equals(normalizedType)) {
            throw new IllegalArgumentException("Unsupported OTP type.");
        }
        
        String normalizedEmail = value != null ? value.trim().toLowerCase() : "";
        if (normalizedEmail.isEmpty()) {
            throw new IllegalArgumentException("Email address is required for OTP.");
        }

        AppUser appUser = resolveUser(username);
        String otp = generateAndStore(normalizedEmail, appUser != null ? appUser.getId() : null);
        
        // Log the actual OTP for debugging or local verification in case SMTP is blocked
        log.info("[OTP] Generated OTP for {}: {}", mask(normalizedEmail), otp);
        log.info("[OTP] Sending {} OTP to {} (appUserId: {})", 
                normalizedType, mask(normalizedEmail), appUser != null ? appUser.getId() : "GUEST");
                
        emailService.sendEmailOtpAsync(mailFrom, normalizedEmail, otp, otpExpiryMinutes);
    }

    public void verifyOtp(String type, String value, String otp) {
        verifyOtp(type, value, otp, null);
    }

    @Transactional
    public void verifyOtp(String type, String value, String otp, String username) {
        String normalizedType = type != null ? type.trim().toUpperCase() : "";
        if (!"EMAIL".equals(normalizedType) && !"FORGOT_PASSWORD".equals(normalizedType)) {
            throw new IllegalArgumentException("Unsupported OTP type.");
        }

        String normalizedEmail = value != null ? value.trim().toLowerCase() : "";
        AppUser appUser = resolveUser(username);
        String key = buildKey(normalizedEmail, appUser != null ? appUser.getId() : null);
        
        String stored = redisTemplate.opsForValue().get(key);
        
        if (stored == null) {
            log.warn("[OTP] Verification failed: No code found for key={}", key);
            throw new IllegalArgumentException("OTP expired or not found. Please resend.");
        }

        if (!stored.equals(otp != null ? otp.trim() : "")) {
            log.warn("[OTP] Verification failed: Code mismatch for email={}", mask(normalizedEmail));
            throw new IllegalArgumentException("Invalid verification code.");
        }

        // Consume — one-time use only
        redisTemplate.delete(key);

        // Mark account as verified
        if (appUser == null) {
            // User not yet created (e.g. during signup flow)
            // Store verification success in Redis for AuthService to use
            redisTemplate.opsForValue().set("otp:verified:" + normalizedEmail, "true", Duration.ofMinutes(15));
            log.info("[OTP] Pre-verified email for signup: {}", mask(value));
            return;
        }

        // Fetch fresh managed entity to ensure persistence
        AppUser managedUser = appUserRepository.findById(appUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found during verification."));

        if (managedUser.getUserProfile() != null) {
            User profile = managedUser.getUserProfile();
            profile.setIsVerified(true);
            // Explicitly save the profile first
            userRepository.save(profile);
            // Then save and flush the app user
            appUserRepository.saveAndFlush(managedUser);
            log.info("[OTP] Account verified and persisted: appUserId={} profileId={} email={}", 
                    managedUser.getId(), profile.getId(), mask(value));
        } else {
            log.warn("[OTP] User has no profile to verify: appUserId={}", managedUser.getId());
        }
    }

    private String generateAndStore(String value, Long appUserId) {
        int code = 100_000 + RANDOM.nextInt(900_000);
        String otp = String.valueOf(code);
        String key = buildKey(value, appUserId);
        redisTemplate.opsForValue().set(key, otp, Duration.ofMinutes(otpExpiryMinutes));
        log.debug("[OTP] Stored key={} ttl={}min", key, otpExpiryMinutes);
        return otp;
    }

    private AppUser resolveUser(String username) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AppUser appUser) {
            return appUser;
        }
        if (username != null && !username.isBlank()) {
            AppUser found = appUserRepository.findByUsername(username);
            if (found != null) return found;
        }
        return null;
    }

    private String buildKey(String value, Long appUserId) {
        String base = KEY_PREFIX + "email:" + value.trim();
        return appUserId != null ? base + ":" + appUserId : base;
    }

    /** Masks PII in logs — never log emails in full in production. */
    private String mask(String value) {
        if (value == null || value.length() < 4) return "***";
        if (value.contains("@")) {
            int at = value.indexOf('@');
            return value.substring(0, Math.min(2, at)) + "***" + value.substring(at);
        }
        return value.substring(0, 2) + "****" + value.substring(value.length() - 2);
    }
}