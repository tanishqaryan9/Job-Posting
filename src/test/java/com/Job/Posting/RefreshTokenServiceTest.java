package com.Job.Posting;

import com.Job.Posting.entity.AppUser;
import com.Job.Posting.entity.RefreshToken;
import com.Job.Posting.entity.type.AuthProviderType;
import com.Job.Posting.refresh.repository.RefreshTokenRepository;
import com.Job.Posting.refresh.service.RefreshTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks private RefreshTokenService refreshTokenService;

    private AppUser appUser;
    private RefreshToken validToken;

    @BeforeEach
    void setUp() {
        appUser = AppUser.builder()
                .id(1L)
                .username("testuser")
                .password("encodedPassword")
                .providerType(AuthProviderType.EMAIL)
                .build();

        validToken = RefreshToken.builder()
                .id(1L)
                .token(UUID.randomUUID().toString())
                .appUser(appUser)
                .expiresAt(Instant.now().plusSeconds(3600))
                .used(false)
                .build();
    }

    @Test
    void createRefreshToken_shouldDeleteOldAndCreateNew() {
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(validToken);

        RefreshToken result = refreshTokenService.createRefreshToken(appUser);

        verify(refreshTokenRepository).deleteByAppUser(appUser);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
        assertThat(result).isNotNull();
        assertThat(result.getAppUser()).isEqualTo(appUser);
    }

    @Test
    void rotateRefreshToken_shouldReturnNewToken_whenValid() {
        RefreshToken newToken = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .appUser(appUser)
                .expiresAt(Instant.now().plusSeconds(3600))
                .used(false)
                .build();

        when(refreshTokenRepository.findByToken(validToken.getToken()))
                .thenReturn(Optional.of(validToken));
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenReturn(validToken)   // saving old as used
                .thenReturn(newToken);    // saving new token

        RefreshToken result = refreshTokenService.rotateRefreshToken(validToken.getToken());

        assertThat(result).isNotNull();
        assertThat(validToken.isUsed()).isTrue();
        verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
    }

    @Test
    void rotateRefreshToken_shouldThrow_whenTokenAlreadyUsed() {
        validToken.setUsed(true);
        when(refreshTokenRepository.findByToken(validToken.getToken()))
                .thenReturn(Optional.of(validToken));

        assertThatThrownBy(() -> refreshTokenService.rotateRefreshToken(validToken.getToken()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("reuse detected");

        verify(refreshTokenRepository).deleteAllByAppUser(appUser);
    }

    @Test
    void rotateRefreshToken_shouldThrow_whenTokenExpired() {
        RefreshToken expiredToken = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .appUser(appUser)
                .expiresAt(Instant.now().minusSeconds(3600)) // expired
                .used(false)
                .build();

        when(refreshTokenRepository.findByToken(expiredToken.getToken()))
                .thenReturn(Optional.of(expiredToken));

        assertThatThrownBy(() -> refreshTokenService.rotateRefreshToken(expiredToken.getToken()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expired");

        verify(refreshTokenRepository).delete(expiredToken);
    }

    @Test
    void rotateRefreshToken_shouldThrow_whenTokenNotFound() {
        when(refreshTokenRepository.findByToken("invalid-token"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.rotateRefreshToken("invalid-token"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid refresh token");
    }

    @Test
    void deleteByUser_shouldCallRepository() {
        refreshTokenService.deleteByUser(appUser);
        verify(refreshTokenRepository).deleteAllByAppUser(appUser);
    }
}
