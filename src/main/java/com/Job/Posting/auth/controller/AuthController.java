package com.Job.Posting.auth.controller;

import com.Job.Posting.auth.service.AuthService;
import com.Job.Posting.dto.otp.OtpRequestDto;
import com.Job.Posting.dto.otp.OtpVerifyDto;
import com.Job.Posting.dto.refresh.RefreshRequestDto;
import com.Job.Posting.dto.security.LoginRequestDto;
import com.Job.Posting.dto.security.LoginResponseDto;
import com.Job.Posting.dto.security.SignupRequestDto;
import com.Job.Posting.dto.security.SignupResponseDto;
import com.Job.Posting.otp.service.OtpService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final OtpService  otpService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@RequestBody @Valid LoginRequestDto loginRequestDto)
    {
        return ResponseEntity.ok(authService.login(loginRequestDto));
    }

    @PostMapping("/signup")
    public ResponseEntity<SignupResponseDto> signup(@RequestBody @Valid SignupRequestDto signupRequestDto)
    {
        return ResponseEntity.ok(authService.signup(signupRequestDto));
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponseDto> refresh(@RequestBody @Valid RefreshRequestDto refreshRequestDto)
    {
        return ResponseEntity.ok(authService.refresh(refreshRequestDto));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody @Valid RefreshRequestDto refreshRequestDto)
    {
        authService.logout(refreshRequestDto);
        return ResponseEntity.noContent().build();
    }

    // ── Availability check (public, no JWT needed) ────────────────────────────

    @GetMapping("/check-availability")
    public ResponseEntity<Map<String, Object>> checkAvailability(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String phone) {
        return ResponseEntity.ok(authService.checkAvailability(username, phone));
    }

    // ── OTP ───────────────────────────────────────────────────────────────────

    @PostMapping("/otp/send")
    public ResponseEntity<Map<String, String>> sendOtp(@RequestBody @Valid OtpRequestDto req)
    {
        otpService.sendOtp(req.getType(), req.getValue(), req.getUsername());
        return ResponseEntity.ok(Map.of(
                "message", "OTP sent successfully to " + req.getValue()
        ));
    }

    @PostMapping("/otp/verify")
    public ResponseEntity<Map<String, String>> verifyOtp(@RequestBody @Valid OtpVerifyDto req)
    {
        otpService.verifyOtp(req.getType(), req.getValue(), req.getOtp(), req.getUsername());
        return ResponseEntity.ok(Map.of(
                "message", req.getType() + " verified successfully"
        ));
    }
}