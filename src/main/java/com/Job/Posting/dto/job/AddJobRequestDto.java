package com.Job.Posting.dto.job;

import com.Job.Posting.entity.User;
import com.Job.Posting.entity.type.JobType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AddJobRequestDto {

    @NotBlank(message = "Title cannot be blank")
    @Size(max = 100, message = "Title cannot exceed 100 characters")
    private String title;

    @Size(max = 300, message = "Description cannot exceed 300 characters")
    private String description;

    @Min(value = 0, message = "Salary cannot be negative")
    private Double salary;

    @Size(max = 100, message = "Location cannot exceed 100 characters")
    private String location;

    private JobType job_type;

    @Min(value = 0, message = "Experience cannot be negative")
    @Max(value = 50, message = "Experience cannot exceed 50 years")
    private Integer experience_required;

    private Double latitude;
    private Double longitude;

    @NotNull(message = "User ID cannot be null")
    private Long createdByUserId;
}
