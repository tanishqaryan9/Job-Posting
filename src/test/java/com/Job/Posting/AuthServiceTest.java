package com.Job.Posting;

import com.Job.Posting.auth.repository.AppUserRepository;
import com.Job.Posting.auth.service.AuthService;
import com.Job.Posting.dto.refresh.RefreshRequestDto;
import com.Job.Posting.dto.security.LoginRequestDto;
import com.Job.Posting.dto.security.LoginResponseDto;
import com.Job.Posting.dto.security.SignupRequestDto;
import com.Job.Posting.dto.security.SignupResponseDto;
import com.Job.Posting.entity.AppUser;
import com.Job.Posting.entity.RefreshToken;
import com.Job.Posting.entity.User;
import com.Job.Posting.entity.type.AuthProviderType;
import com.Job.Posting.refresh.repository.RefreshTokenRepository;
import com.Job.Posting.refresh.service.RefreshTokenService;
import com.Job.Posting.security.AuthUtil;
import com.Job.Posting.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private AuthUtil authUtil;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private AppUserRepository appUserRepository;
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks private AuthService authService;

    private AppUser appUser;
    private User userProfile;
    private RefreshToken refreshToken;

    @BeforeEach
    void setUp() {
        userProfile = new User();
        userProfile.setId(10L);
        userProfile.setName("Test User");
        userProfile.setNumber("9999999999");
        userProfile.setLocation("Delhi");
        userProfile.setExperience(2);

        appUser = AppUser.builder()
                .id(1L)
                .username("testuser@gmail.com")
                .password("encodedPassword")
                .providerType(AuthProviderType.EMAIL)
                .userProfile(userProfile)
                .build();

        refreshToken = RefreshToken.builder()
                .id(1L)
                .token(UUID.randomUUID().toString())
                .appUser(appUser)
                .expiresAt(Instant.now().plusSeconds(3600))
                .used(false)
                .build();
    }

    // ── login ─────────────────────────────────────────────────────────────────

    @Test
    void login_shouldReturnTokensAndProfileId_whenCredentialsValid() {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(appUser);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(auth);
        when(authUtil.GenerateAccessToken(appUser)).thenReturn("access-token");
        when(refreshTokenService.createRefreshToken(appUser)).thenReturn(refreshToken);

        LoginResponseDto result = authService.login(new LoginRequestDto("testuser@gmail.com", "password"));

        assertThat(result).isNotNull();
        assertThat(result.getAccessToken()).isEqualTo("access-token");
        assertThat(result.getRefreshToken()).isEqualTo(refreshToken.getToken());
        assertThat(result.getAppUserId()).isEqualTo(1L);
        assertThat(result.getProfileId()).isEqualTo(10L);
    }

    @Test
    void login_shouldReturnNullProfileId_whenNoProfileLinked() {
        appUser.setUserProfile(null);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(appUser);
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(authUtil.GenerateAccessToken(appUser)).thenReturn("access-token");
        when(refreshTokenService.createRefreshToken(appUser)).thenReturn(refreshToken);

        LoginResponseDto result = authService.login(new LoginRequestDto("testuser@gmail.com", "password"));

        assertThat(result.getProfileId()).isNull();
    }

    @Test
    void login_shouldThrow_whenCredentialsInvalid() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(new LoginRequestDto("wrong", "wrong")))
                .isInstanceOf(BadCredentialsException.class);
    }

    // ── signup ────────────────────────────────────────────────────────────────

    @Test
    void signup_shouldCreateBothProfileAndAppUser() {
        SignupRequestDto requestDto = new SignupRequestDto(
                "newuser@gmail.com", "Password@123",
                "New User", "9876543210", "Mumbai", 3, 19.0, 72.8
        );

        // Username not taken
        when(appUserRepository.findByUsername("newuser@gmail.com")).thenReturn(null);
        // Profile save returns userProfile (id=10)
        when(userRepository.save(any(User.class))).thenReturn(userProfile);
        when(passwordEncoder.encode("Password@123")).thenReturn("encodedPassword");
        // AppUser save returns our stub appUser (id=1, username=testuser@gmail.com)
        when(appUserRepository.save(any(AppUser.class))).thenReturn(appUser);

        SignupResponseDto result = authService.signup(requestDto);

        assertThat(result).isNotNull();
        // Result reflects what the stub returns (appUser.getUsername())
        assertThat(result.getUsername()).isEqualTo("testuser@gmail.com");
        assertThat(result.getProfileId()).isEqualTo(10L);
        assertThat(result.getAppUserId()).isEqualTo(1L);

        verify(userRepository).save(any(User.class));
        verify(appUserRepository).save(any(AppUser.class));
    }

    @Test
    void signup_shouldThrow_whenUsernameAlreadyExists() {
        when(appUserRepository.findByUsername("testuser@gmail.com")).thenReturn(appUser);

        SignupRequestDto requestDto = new SignupRequestDto(
                "testuser@gmail.com", "Password@123",
                "Test", "1234567890", "Delhi", 1, null, null
        );

        assertThatThrownBy(() -> authService.signup(requestDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");

        // Profile must NOT be created if username is taken
        verify(userRepository, never()).save(any());
    }

    // ── logout ────────────────────────────────────────────────────────────────

    @Test
    void logout_shouldDeleteTokens_whenValid() {
        when(refreshTokenRepository.findByToken(refreshToken.getToken()))
                .thenReturn(Optional.of(refreshToken));

        authService.logout(new RefreshRequestDto(refreshToken.getToken()));

        verify(refreshTokenService).deleteByUser(appUser);
    }

    @Test
    void logout_shouldThrow_whenTokenInvalid() {
        when(refreshTokenRepository.findByToken("invalid")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.logout(new RefreshRequestDto("invalid")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid refresh token");
    }

    // ── refresh ───────────────────────────────────────────────────────────────

    @Test
    void refresh_shouldReturnNewTokens_whenValid() {
        RefreshToken newToken = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .appUser(appUser)
                .expiresAt(Instant.now().plusSeconds(3600))
                .used(false)
                .build();

        when(refreshTokenService.rotateRefreshToken(refreshToken.getToken())).thenReturn(newToken);
        when(authUtil.GenerateAccessToken(appUser)).thenReturn("new-access-token");

        LoginResponseDto result = authService.refresh(new RefreshRequestDto(refreshToken.getToken()));

        assertThat(result.getAccessToken()).isEqualTo("new-access-token");
        assertThat(result.getRefreshToken()).isEqualTo(newToken.getToken());
    }
}