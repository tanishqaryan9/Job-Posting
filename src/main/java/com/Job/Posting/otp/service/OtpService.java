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

    /**
     * Sends the OTP email synchronously on the calling thread.
     *
     * IMPORTANT: @Async cannot be placed on a private method — Spring's CGLIB proxy
     * cannot override private methods, so the annotation is silently ignored.
     * The method intentionally runs synchronously so that any SMTP failure
     * propagates back to the caller as a real exception (and the API returns a
     * proper error to the client instead of a silent 200 OK with an undelivered email).
     *
     * If async behaviour is ever needed, extract this into a separate Spring bean
     * with a public method annotated @Async.
     */
    private void sendEmailOtp(String email, String otp) {
        // FIX: never fall back to a fake "noreply@jobhunt.com" domain — that From address
        // mismatches the authenticated Gmail account and triggers spam filters.
        // If mailFrom is not configured the application is mis-configured; fail fast.
        if (mailFrom == null || mailFrom.isBlank()) {
            log.error("[OTP] MAIL_USERNAME is not configured. Cannot send OTP email.");
            throw new RuntimeException("Email service is not configured. Please contact support.");
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            // MimeMessageHelper(msg, multipart, charset) — must pass charset here
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(mailFrom);           // must exactly match the SMTP auth account
            helper.setTo(email);
            helper.setSubject("Your JobHunt Verification Code");

            // Plain-text alternative — required for non-HTML clients and spam compliance
            String plain = "Your JobHunt verification code is: " + otp
                    + "\n\nThis code expires in " + otpExpiryMinutes + " minutes."
                    + "\n\nIf you did not request this, please ignore this email.";

            String html = """
                    <div style="font-family:Arial,sans-serif;max-width:520px;margin:0 auto;padding:24px;border:1px solid #e5e7eb;border-radius:12px;background:#fff">
                      <h2 style="margin:0 0 4px;color:#1e293b">JobHunt Platform</h2>
                      <p style="margin:0 0 24px;color:#64748b;font-size:13px">Account verification</p>
                      <div style="background:#f8fafc;border-radius:8px;padding:24px;text-align:center">
                        <p style="margin:0 0 12px;color:#334155;font-size:15px">Your verification code</p>
                        <div style="font-size:40px;font-weight:700;letter-spacing:8px;color:#1e293b;font-family:monospace">%s</div>
                        <p style="margin:16px 0 0;color:#64748b;font-size:13px">Expires in <strong>%d minutes</strong></p>
                      </div>
                      <p style="margin:24px 0 0;color:#94a3b8;font-size:11px;text-align:center">If you did not request this code, please ignore this email.</p>
                    </div>
                    """.formatted(otp, otpExpiryMinutes);

            // setText(html, plain, htmlCharset) — sets both HTML and plain-text parts
            helper.setText(plain, html);

            mailSender.send(mimeMessage);
            log.info("[OTP] Email OTP dispatched to {}", mask(email));

        } catch (Exception e) {
            // FIX: Log the full exception (not just getMessage()) so the stack trace
            // appears in the logs, then rethrow so the API returns a real error
            // instead of a silent 200 OK when delivery fails.
            log.error("[OTP] Failed to send OTP email to {}", mask(email), e);
            throw new RuntimeException("Failed to send OTP email. Please try again.", e);
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