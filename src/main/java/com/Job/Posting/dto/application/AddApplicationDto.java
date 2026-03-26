package com.Job.Posting.dto.application;

import com.Job.Posting.entity.Job;
import com.Job.Posting.entity.User;
import com.Job.Posting.entity.type.StatusType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddApplicationDto {

    @NotNull(message = "Job ID cannot be null")
    private Long jobId;

    @NotNull(message = "User ID cannot be null")
    private Long userId;
}
