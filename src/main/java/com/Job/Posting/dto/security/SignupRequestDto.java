package com.Job.Posting.dto.security;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SignupRequestDto {

    @NotBlank(message = "Username cannot be blank")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Pattern(regexp = "^[a-z0-9_.]+$", message = "Username must contain only lowercase letters, numbers, underscores, or periods")
    private String username;

    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    // Profile fields — collected at signup
    @NotBlank(message = "Name cannot be blank")
    @Size(max = 100, message = "Name cannot exceed 100 characters")
    private String name;

    @NotBlank(message = "Phone number cannot be blank")
    @Size(min = 10, max = 10, message = "Number must be 10 digits")
    @Pattern(regexp = "^[0-9]{10}$", message = "Number must contain only digits")
    private String number;

    @NotBlank(message = "Location cannot be blank")
    @Size(max = 100, message = "Location cannot exceed 100 characters")
    private String location;

    @NotNull(message = "Experience cannot be null")
    @Min(value = 0, message = "Experience cannot be negative")
    @Max(value = 50, message = "Experience cannot exceed 50 years")
    private Integer experience;

    // Optional profile fields
    private Double latitude;
    private Double longitude;
}
