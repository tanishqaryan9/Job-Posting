package com.Job.Posting.auth.service;

import com.Job.Posting.auth.repository.AppUserRepository;
import com.Job.Posting.dto.refresh.RefreshRequestDto;
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
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
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
    private final PasswordEncoder passwordEncoder;
    private final ModelMapper modelMapper;
    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenRepository refreshTokenRepository;

    public LoginResponseDto login(LoginRequestDto loginRequestDto) {

        Authentication authorization = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginRequestDto.getUsername(),loginRequestDto.getPassword()));

        AppUser user = (AppUser) authorization.getPrincipal();

        String accessToken = authUtil.GenerateAccessToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        return new LoginResponseDto(user.getId(),accessToken,refreshToken.getToken());
    }

    public LoginResponseDto refresh(RefreshRequestDto refreshRequestDto) {

        RefreshToken newRefreshToken = refreshTokenService.rotateRefreshToken(refreshRequestDto.getRefreshToken());

        String newAccessToken = authUtil.GenerateAccessToken(newRefreshToken.getAppUser());

        return new LoginResponseDto(newRefreshToken.getAppUser().getId(), newAccessToken, newRefreshToken.getToken());
    }

    private AppUser signUpInternal(SignupRequestDto signupRequestDto)
    {
        AppUser user= appUserRepository.findByUsername(signupRequestDto.getUsername());
        if(user!=null)
        {
            throw new IllegalArgumentException("User already exists");
        }
        return appUserRepository.save(AppUser.builder().username(signupRequestDto.getUsername()).password(passwordEncoder.encode(signupRequestDto.getPassword())).build());
    }

    public void logout(RefreshRequestDto refreshRequestDto) {

        RefreshToken token = refreshTokenRepository.findByToken(refreshRequestDto.getRefreshToken()).orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));
        refreshTokenService.deleteByUser(token.getAppUser());
    }

    public SignupResponseDto signup(SignupRequestDto signupRequestDto) {

        AppUser appUser = signUpInternal(signupRequestDto);
        return modelMapper.map(appUser,SignupResponseDto.class);
    }

    public ResponseEntity<LoginResponseDto> handleOAuh2LoginRequests(OAuth2User user, String registrationID) {
        //fetch providerType and providerID
        //save providerType and providerID in DB sp user cannot log in with both Google and then GitHub
        //if the user has account: directly log in
        //otherwise: first signup and then login
        AuthProviderType providerType = authUtil.getProviderTypeFromRegistrationID(registrationID);
        String providerId = authUtil.determineProviderIDFromOAuth2User(user,registrationID);

        AppUser user1 = appUserRepository.findByProviderIdAndProviderType(providerId,providerType);

        String email = user.getAttribute("email");
        AppUser emailUser = appUserRepository.findByUsername(email);

        if(emailUser==null && user1==null)
        {
            String username = authUtil.determineUsernameFromOAuth2User(user,registrationID,providerId);
            //If provider is Google then there is no need to store password
            user1 = signUpInternal(new SignupRequestDto(username,null));
        }
        else if(emailUser!=null)
        {
            if(email!=null && !email.isBlank() && !email.equals(user1.getUsername()))
            {
                user1.setUsername(email);
                appUserRepository.save(user1);
            }
        }
        else
        {
            throw new BadCredentialsException("This email is already registered with provider "+ emailUser.getProviderType());
        }

        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user1);

        LoginResponseDto loginResponseDto = new LoginResponseDto(user1.getId(),authUtil.GenerateAccessToken(user1),refreshToken.getToken());
        return ResponseEntity.ok(loginResponseDto);
    }
}
