package com.Job.Posting.dto.user;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class AddUserRequestDto {

    @NotBlank(message = "Name cannot be blank")
    @Size(max = 100, message = "Name cannot exceed 100 characters")
    private String name;

    @NotBlank(message = "Number cannot be blank")
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

    private String profile_photo;

    private Double latitude;
    private Double longitude;
}
