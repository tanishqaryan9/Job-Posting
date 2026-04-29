package com.Job.Posting.otp.service;

import com.Job.Posting.auth.repository.AppUserRepository;
import com.Job.Posting.entity.AppUser;
import com.Job.Posting.entity.User;
import com.Job.Posting.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
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
    private final UserRepository userRepository;

    // ── Public API ────────────────────────────────────────────────────────────

    public void sendOtp(String type, String value) {
        sendOtp(type, value, null);
    }

    public void sendOtp(String type, String value, String username) {
        if (!"EMAIL".equalsIgnoreCase(type)) {
            throw new IllegalArgumentException("Only EMAIL OTP is supported.");
        }
        AppUser appUser = resolveUser(username);
        String otp = generateAndStore(value, appUser != null ? appUser.getId() : null);
        sendEmailOtp(value, otp);
    }

    public void verifyOtp(String type, String value, String otp) {
        verifyOtp(type, value, otp, null);
    }

    @Transactional
    public void verifyOtp(String type, String value, String otp, String username) {
        if (!"EMAIL".equalsIgnoreCase(type)) {
            throw new IllegalArgumentException("Only EMAIL OTP is supported.");
        }

        AppUser appUser = resolveUser(username);

        String key = buildKey(value, appUser != null ? appUser.getId() : null);
        String stored = redisTemplate.opsForValue().get(key);

        if (stored == null) {
            throw new IllegalArgumentException("OTP expired or not found. Please request a new one.");
        }
        if (!stored.equals(otp.trim())) {
            throw new IllegalArgumentException("Incorrect OTP. Please check and try again.");
        }

        // One-time use — consume immediately
        redisTemplate.delete(key);

        if (appUser == null) {
            log.warn("[OTP] OTP consumed but no user resolved for value={}", mask(value));
            return;
        }

        // Re-fetch managed entity to ensure JPA tracks changes
        AppUser managedUser = appUserRepository.findById(appUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found during verification."));

        User profile = managedUser.getUserProfile();
        if (profile == null) {
            log.warn("[OTP] No profile found for appUserId={} — cannot mark verified", managedUser.getId());
            throw new IllegalArgumentException("User profile not set up yet. Please complete your profile first.");
        }

        // Mark account as verified and store the verified email address
        profile.setIsVerified(true);
        profile.setEmail(value.trim().toLowerCase());
        userRepository.save(profile);

        log.info("[OTP] Account verified: appUserId={} profileId={} email={}",
                managedUser.getId(), profile.getId(), mask(value));
    }

    // ── Internals ─────────────────────────────────────────────────────────────

    private String generateAndStore(String value, Long appUserId) {
        int code = 100_000 + RANDOM.nextInt(900_000);
        String otp = String.valueOf(code);
        String key = buildKey(value, appUserId);
        redisTemplate.opsForValue().set(key, otp, Duration.ofMinutes(otpExpiryMinutes));
        log.debug("[OTP] Stored key={} ttl={}min", key, otpExpiryMinutes);
        return otp;
    }

    private void sendEmailOtp(String email, String otp) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            if (mailFrom != null && !mailFrom.isBlank()) {
                msg.setFrom(mailFrom);
            }
            msg.setTo(email);
            msg.setSubject("Your JobPosting Verification Code");
            msg.setText(
                    "Your verification code is: " + otp + "\n\n" +
                            "This code expires in " + otpExpiryMinutes + " minutes.\n\n" +
                            "If you did not request this, please ignore this email."
            );
            mailSender.send(msg);
            log.info("[OTP] Email OTP sent to {}", mask(email));
        } catch (Exception e) {
            log.error("[OTP] Failed to send email to {}: {}", mask(email), e.getMessage());
            throw new RuntimeException("Failed to send OTP email. Please try again.");
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
        String base = KEY_PREFIX + "email:" + value.trim().toLowerCase();
        return appUserId != null ? base + ":" + appUserId : base;
    }

    private String mask(String value) {
        if (value == null || value.length() < 4) return "***";
        if (value.contains("@")) {
            int at = value.indexOf('@');
            return value.substring(0, Math.min(2, at)) + "***" + value.substring(at);
        }
        return value.substring(0, 2) + "****" + value.substring(value.length() - 2);
    }
}