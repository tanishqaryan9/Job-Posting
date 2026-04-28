package com.Job.Posting.dto.otp;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

public class OtpDtos {

    @Getter @Setter
    public static class SendOtpRequest {

        @NotBlank
        @Pattern(regexp = "EMAIL", message = "type must be EMAIL")
        private String type;

        @NotBlank
        private String value;

        private String username;
    }

    @Getter @Setter
    public static class VerifyOtpRequest {

        @NotBlank
        @Pattern(regexp = "EMAIL", message = "type must be EMAIL")
        private String type;

        @NotBlank
        private String value;

        @NotBlank
        private String otp;

        private String username;
    }

    @Getter @Setter
    public static class OtpResponse {
        private String message;
        public OtpResponse(String message) { this.message = message; }
    }
}