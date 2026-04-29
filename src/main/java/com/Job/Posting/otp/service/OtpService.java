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

    @Value("${spring.mail.username:}")
    private String mailFrom;

    private final StringRedisTemplate redisTemplate;
    private final JavaMailSender mailSender;
    private final AppUserRepository appUserRepository;
    private final com.Job.Posting.user.repository.UserRepository userRepository;

    // ── Public API ────────────────────────────────────────────────────────────

    public void sendOtp(String type, String value) {
        sendOtp(type, value, null);
    }

    public void sendOtp(String type, String value, String username) {
        String normalizedType = type != null ? type.trim().toUpperCase() : "";
        if (!"EMAIL".equals(normalizedType)) {
            throw new IllegalArgumentException("Only EMAIL OTP is supported.");
        }
        
        String normalizedEmail = value != null ? value.trim().toLowerCase() : "";
        if (normalizedEmail.isEmpty()) {
            throw new IllegalArgumentException("Email address is required for OTP.");
        }

        AppUser appUser = resolveUser(username);
        String otp = generateAndStore(normalizedEmail, appUser != null ? appUser.getId() : null);
        
        log.info("[OTP] Sending {} OTP to {} (appUserId: {})", 
                normalizedType, mask(normalizedEmail), appUser != null ? appUser.getId() : "GUEST");
                
        sendEmailOtp(normalizedEmail, otp);
    }

    public void verifyOtp(String type, String value, String otp) {
        verifyOtp(type, value, otp, null);
    }

    @Transactional
    public void verifyOtp(String type, String value, String otp, String username) {
        String normalizedType = type != null ? type.trim().toUpperCase() : "";
        if (!"EMAIL".equals(normalizedType)) {
            throw new IllegalArgumentException("Only EMAIL OTP is supported.");
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
            log.error("[OTP] Verified code but could not resolve user. value={}", mask(value));
            throw new IllegalArgumentException("User session lost. Please log in again and verify.");
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

    @org.springframework.scheduling.annotation.Async
    private void sendEmailOtp(String email, String otp) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");
            
            String from = (mailFrom != null && !mailFrom.isBlank()) ? mailFrom : "noreply@jobhunt.com";
            helper.setFrom(from, "JobHunt Platform");
            helper.setTo(email);
            helper.setSubject("Your JobHunt Verification Code: " + otp);
            
            String htmlContent = """
                <div style="font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 12px; background-color: #ffffff;">
                    <div style="text-align: center; margin-bottom: 30px;">
                        <h2 style="color: #3b82f6; margin: 0;">JobHunt Platform</h2>
                        <p style="color: #6b7280; font-size: 14px; margin-top: 5px;">Secure Verification</p>
                    </div>
                    <div style="padding: 30px; background-color: #f8fafc; border-radius: 8px; text-align: center;">
                        <p style="font-size: 16px; color: #334155; margin-bottom: 20px;">Your verification code is:</p>
                        <div style="font-size: 42px; font-weight: 700; letter-spacing: 5px; color: #1e293b; margin: 20px 0; font-family: monospace;">%s</div>
                        <p style="font-size: 14px; color: #64748b; margin-top: 20px;">This code expires in <strong>%d minutes</strong>.</p>
                    </div>
                    <div style="margin-top: 30px; color: #94a3b8; font-size: 12px; text-align: center; line-height: 1.6;">
                        <p>If you did not request this code, please ignore this email.</p>
                        <p>&copy; 2026 JobHunt Platform. All rights reserved.</p>
                    </div>
                </div>
                """.formatted(otp, otpExpiryMinutes);

            helper.setText(htmlContent, true);
            
            mailSender.send(mimeMessage);
            log.info("[OTP] Rich HTML Email OTP sent to {}", mask(email));
        } catch (Exception e) {
            log.error("[OTP] Failed to send rich email OTP to {}: {}", mask(email), e.getMessage());
        }
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