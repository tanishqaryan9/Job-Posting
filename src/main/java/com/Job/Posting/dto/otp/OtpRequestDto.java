package com.Job.Posting.dto.otp;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OtpRequestDto {

    @NotBlank(message = "type is required (must be EMAIL)")
    private String type;

    @NotBlank(message = "value is required")
    private String value;

    private String username;
}