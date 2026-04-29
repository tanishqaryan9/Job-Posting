package com.Job.Posting.dto.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponseDto {

    private Long appUserId;
    private Long profileId;
    private String accessToken;
    private String refreshToken;
    private String username;
    private String oauthName;
}