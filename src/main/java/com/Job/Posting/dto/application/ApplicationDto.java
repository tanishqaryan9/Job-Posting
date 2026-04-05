package com.Job.Posting.dto.application;

import com.Job.Posting.dto.job.JobDto;
import com.Job.Posting.dto.user.UserDto;
import com.Job.Posting.entity.type.StatusType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApplicationDto {

    private Long id;
    private JobDto job;
    private UserDto user;
    private StatusType status;
    private LocalDateTime applied_at;
}
