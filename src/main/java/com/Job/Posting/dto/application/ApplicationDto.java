package com.Job.Posting.dto.application;

import com.Job.Posting.entity.Job;
import com.Job.Posting.entity.User;
import com.Job.Posting.entity.type.StatusType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApplicationDto {

    private Long id;
    private Job job;
    private User user;
    private StatusType status;
    private LocalDateTime applied_at;
}
