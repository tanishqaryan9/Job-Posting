package com.Job.Posting.auth.controller;

import com.Job.Posting.auth.service.AuthService;
import com.Job.Posting.dto.refresh.RefreshRequestDto;
import com.Job.Posting.dto.security.LoginRequestDto;
import com.Job.Posting.dto.security.LoginResponseDto;
import com.Job.Posting.dto.security.SignupRequestDto;
import com.Job.Posting.dto.security.SignupResponseDto;
import com.Job.Posting.entity.AppUser;
import com.Job.Posting.entity.RefreshToken;
import com.Job.Posting.refresh.repository.RefreshTokenRepository;
import com.Job.Posting.refresh.service.RefreshTokenService;
import com.Job.Posting.security.AuthUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@RequestBody LoginRequestDto loginRequestDto)
    {
        return ResponseEntity.ok(authService.login(loginRequestDto));
    }

    @PostMapping("/signup")
    public ResponseEntity<SignupResponseDto> login(@RequestBody SignupRequestDto signupRequestDto)
    {
        return ResponseEntity.ok(authService.signup(signupRequestDto));
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponseDto> refresh(@RequestBody RefreshRequestDto refreshRequestDto)
    {
        return ResponseEntity.ok(authService.refresh(refreshRequestDto));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody RefreshRequestDto refreshRequestDto)
    {
        authService.logout(refreshRequestDto);
        return ResponseEntity.noContent().build();
    }
}
