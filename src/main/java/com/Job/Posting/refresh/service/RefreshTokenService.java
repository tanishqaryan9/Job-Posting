package com.Job.Posting.refresh.service;

import com.Job.Posting.entity.AppUser;
import com.Job.Posting.entity.RefreshToken;
import com.Job.Posting.refresh.repository.RefreshTokenRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final long REFRESH_EXPIRY_DAYS = 7;
    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public RefreshToken createRefreshToken(AppUser user)
    {
        refreshTokenRepository.deleteByAppUser(user);

        RefreshToken token = RefreshToken.builder()
                .appUser(user)
                .token(UUID.randomUUID().toString())
                .expiresAt(Instant.now().plusSeconds(60*60*24*REFRESH_EXPIRY_DAYS))
                .build();
        return refreshTokenRepository.save(token);
    }

    @Transactional
    public RefreshToken rotateRefreshToken(String oldTokenString) {
        RefreshToken oldToken = refreshTokenRepository.findByToken(oldTokenString).orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        // Reuse detection: token was already rotated (maybe someone stole it)
        if (oldToken.isUsed()) {
            refreshTokenRepository.deleteAllByAppUser(oldToken.getAppUser());
            throw new IllegalArgumentException("Token reuse detected. All sessions invalidated.");
        }

        if (oldToken.getExpiresAt().isBefore(Instant.now())) {
            refreshTokenRepository.delete(oldToken);
            throw new IllegalArgumentException("Refresh token expired. Please login again.");
        }

        oldToken.setUsed(true);
        refreshTokenRepository.save(oldToken);

        RefreshToken newToken = RefreshToken.builder()
                .appUser(oldToken.getAppUser())
                .token(UUID.randomUUID().toString())
                .expiresAt(Instant.now().plusSeconds(60 * 60 * 24 * REFRESH_EXPIRY_DAYS))
                .build();

        return refreshTokenRepository.save(newToken);
    }

    @Transactional
    public void deleteByUser(AppUser user) {
        refreshTokenRepository.deleteAllByAppUser(user);
    }

    @Transactional
    @Scheduled(cron = "0 0 * * * *")
    public void deleteUsedTokens() {
        refreshTokenRepository.deleteAllByUsedTrue();
    }
}
