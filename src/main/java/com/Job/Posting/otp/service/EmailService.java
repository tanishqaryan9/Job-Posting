package com.Job.Posting.otp.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Async
    public void sendEmailOtpAsync(String mailFrom, String email, String otp, long otpExpiryMinutes) {
        if (mailFrom == null || mailFrom.isBlank()) {
            log.error("[OTP] MAIL_USERNAME is not configured. Cannot send OTP email.");
            return;
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(mailFrom);
            helper.setTo(email);
            helper.setSubject("Your JobHunt Verification Code");

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

            helper.setText(plain, html);

            mailSender.send(mimeMessage);
            log.info("[OTP] Email OTP successfully dispatched to {}", mask(email));

        } catch (Exception e) {
            log.error("[OTP] Failed to send OTP email to {}: {}", mask(email), e.getMessage(), e);
        }
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
