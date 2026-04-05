package com.Job.Posting.dto.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SignupResponseDto {

    private Long appUserId;
    private Long profileId;
    private String username;
    private String name;
    private String location;
}
