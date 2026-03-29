package com.Job.Posting;

import com.Job.Posting.auth.repository.AppUserRepository;
import com.Job.Posting.auth.service.AuthService;
import com.Job.Posting.dto.security.LoginRequestDto;
import com.Job.Posting.dto.security.LoginResponseDto;
import com.Job.Posting.dto.security.SignupRequestDto;
import com.Job.Posting.dto.security.SignupResponseDto;
import com.Job.Posting.entity.AppUser;
import com.Job.Posting.entity.RefreshToken;
import com.Job.Posting.entity.type.AuthProviderType;
import com.Job.Posting.refresh.repository.RefreshTokenRepository;
import com.Job.Posting.refresh.service.RefreshTokenService;
import com.Job.Posting.security.AuthUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
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
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private ModelMapper modelMapper;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks private AuthService authService;

    private AppUser appUser;
    private RefreshToken refreshToken;

    @BeforeEach
    void setUp() {
        appUser = AppUser.builder()
                .id(1L)
                .username("testuser")
                .password("encodedPassword")
                .providerType(AuthProviderType.EMAIL)
                .build();

        refreshToken = RefreshToken.builder()
                .id(1L)
                .token(UUID.randomUUID().toString())
                .appUser(appUser)
                .expiresAt(Instant.now().plusSeconds(3600))
                .used(false)
                .build();
    }

    @Test
    void login_shouldReturnTokens_whenCredentialsValid() {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(appUser);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(auth);
        when(authUtil.GenerateAccessToken(appUser)).thenReturn("access-token");
        when(refreshTokenService.createRefreshToken(appUser)).thenReturn(refreshToken);

        LoginResponseDto result = authService.login(new LoginRequestDto("testuser", "password"));

        assertThat(result).isNotNull();
        assertThat(result.getAccessToken()).isEqualTo("access-token");
        assertThat(result.getRefreshToken()).isEqualTo(refreshToken.getToken());
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void login_shouldThrow_whenCredentialsInvalid() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(new LoginRequestDto("wronguser", "wrongpass")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void signup_shouldCreateUser_whenUsernameNotTaken() {
        SignupRequestDto requestDto = new SignupRequestDto("newuser", "password123");
        SignupResponseDto responseDto = new SignupResponseDto();

        when(appUserRepository.findByUsername("newuser")).thenReturn(null);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(appUserRepository.save(any(AppUser.class))).thenReturn(appUser);
        when(modelMapper.map(appUser, SignupResponseDto.class)).thenReturn(responseDto);

        SignupResponseDto result = authService.signup(requestDto);

        assertThat(result).isNotNull();
        verify(appUserRepository).save(any(AppUser.class));
    }

    @Test
    void signup_shouldThrow_whenUsernameAlreadyExists() {
        when(appUserRepository.findByUsername("testuser")).thenReturn(appUser);

        assertThatThrownBy(() -> authService.signup(new SignupRequestDto("testuser", "password123")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void logout_shouldDeleteTokens_whenValid() {
        when(refreshTokenRepository.findByToken(refreshToken.getToken()))
                .thenReturn(Optional.of(refreshToken));

        authService.logout(new com.Job.Posting.dto.refresh.RefreshRequestDto(refreshToken.getToken()));

        verify(refreshTokenService).deleteByUser(appUser);
    }

    @Test
    void logout_shouldThrow_whenTokenInvalid() {
        when(refreshTokenRepository.findByToken("invalid")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.logout(
                new com.Job.Posting.dto.refresh.RefreshRequestDto("invalid")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid refresh token");
    }
}
