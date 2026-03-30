package com.Job.Posting.auth.service;

import com.Job.Posting.auth.repository.AppUserRepository;
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
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthUtil authUtil;
    private final AuthenticationManager authenticationManager;
    private final AppUserRepository appUserRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenRepository refreshTokenRepository;

    public LoginResponseDto login(LoginRequestDto loginRequestDto) {
        Authentication authorization = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequestDto.getUsername(), loginRequestDto.getPassword()));

        AppUser appUser = (AppUser) authorization.getPrincipal();

        String accessToken = authUtil.GenerateAccessToken(appUser);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(appUser);

        Long profileId = appUser.getUserProfile() != null ? appUser.getUserProfile().getId() : null;

        return new LoginResponseDto(appUser.getId(), profileId, accessToken, refreshToken.getToken());
    }

    public LoginResponseDto refresh(RefreshRequestDto refreshRequestDto) {
        RefreshToken newRefreshToken = refreshTokenService.rotateRefreshToken(refreshRequestDto.getRefreshToken());
        AppUser appUser = newRefreshToken.getAppUser();

        String newAccessToken = authUtil.GenerateAccessToken(appUser);
        Long profileId = appUser.getUserProfile() != null ? appUser.getUserProfile().getId() : null;

        return new LoginResponseDto(appUser.getId(), profileId, newAccessToken, newRefreshToken.getToken());
    }

    @Transactional
    public SignupResponseDto signup(SignupRequestDto signupRequestDto) {
        // Check username not already taken
        if (appUserRepository.findByUsername(signupRequestDto.getUsername()) != null) {
            throw new IllegalArgumentException("Username already exists");
        }

        // 1. Create User profile first
        User userProfile = new User();
        userProfile.setName(signupRequestDto.getName());
        userProfile.setNumber(signupRequestDto.getNumber());
        userProfile.setLocation(signupRequestDto.getLocation());
        userProfile.setExperience(signupRequestDto.getExperience());
        userProfile.setLatitude(signupRequestDto.getLatitude());
        userProfile.setLongitude(signupRequestDto.getLongitude());
        User savedProfile = userRepository.save(userProfile);

        // 2. Create AppUser and link to profile
        AppUser appUser = AppUser.builder()
                .username(signupRequestDto.getUsername())
                .password(passwordEncoder.encode(signupRequestDto.getPassword()))
                .providerType(AuthProviderType.EMAIL)
                .providerId(null)
                .userProfile(savedProfile)
                .build();
        AppUser savedAppUser = appUserRepository.save(appUser);

        return new SignupResponseDto(
                savedAppUser.getId(),
                savedProfile.getId(),
                savedAppUser.getUsername(),
                savedProfile.getName(),
                savedProfile.getLocation()
        );
    }

    public void logout(RefreshRequestDto refreshRequestDto) {
        RefreshToken token = refreshTokenRepository.findByToken(refreshRequestDto.getRefreshToken())
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));
        refreshTokenService.deleteByUser(token.getAppUser());
    }

    @Transactional
    public ResponseEntity<LoginResponseDto> handleOAuh2LoginRequests(OAuth2User user, String registrationID) {
        AuthProviderType providerType = authUtil.getProviderTypeFromRegistrationID(registrationID);
        String providerId = authUtil.determineProviderIDFromOAuth2User(user, registrationID);

        AppUser existingAppUser = appUserRepository.findByProviderIdAndProviderType(providerId, providerType);

        String email = user.getAttribute("email");
        AppUser emailUser = appUserRepository.findByUsername(email);

        if (existingAppUser != null) {
            // Returning OAuth2 user — update email if changed
            if (email != null && !email.isBlank()) {
                existingAppUser.setUsername(email);
                appUserRepository.save(existingAppUser);
            }
        } else if (emailUser != null) {
            throw new BadCredentialsException("Email already registered with " + emailUser.getProviderType());
        } else {
            // New OAuth2 user — create AppUser without full profile
            // Profile can be completed later via PUT /users/{id}
            String username = authUtil.determineUsernameFromOAuth2User(user, registrationID, providerId);
            existingAppUser = AppUser.builder()
                    .username(username)
                    .password(null)
                    .providerId(providerId)
                    .providerType(providerType)
                    .userProfile(null) // OAuth2 users complete profile separately
                    .build();
            existingAppUser = appUserRepository.save(existingAppUser);
        }

        RefreshToken refreshToken = refreshTokenService.createRefreshToken(existingAppUser);
        String accessToken = authUtil.GenerateAccessToken(existingAppUser);
        Long profileId = existingAppUser.getUserProfile() != null
                ? existingAppUser.getUserProfile().getId() : null;

        return ResponseEntity.ok(new LoginResponseDto(
                existingAppUser.getId(), profileId, accessToken, refreshToken.getToken()));
    }

    // Internal helper still used by OAuth2 path
    private AppUser signUpInternal(String username, AuthProviderType providerType, String providerId) {
        if (appUserRepository.findByUsername(username) != null) {
            throw new IllegalArgumentException("User already exists");
        }
        return appUserRepository.save(AppUser.builder()
                .username(username)
                .password(null)
                .providerId(providerId)
                .providerType(providerType)
                .userProfile(null)
                .build());
    }
}