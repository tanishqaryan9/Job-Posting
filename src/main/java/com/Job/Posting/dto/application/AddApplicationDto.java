package com.Job.Posting.dto.application;

import com.Job.Posting.entity.type.StatusType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddApplicationDto {

    @NotNull(message = "Job ID cannot be null")
    private Long jobId;


    private String coverLetter;

    private StatusType status;
}
