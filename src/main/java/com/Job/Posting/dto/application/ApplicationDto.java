package com.Job.Posting.dto.application;

import com.Job.Posting.dto.job.JobDto;
import com.Job.Posting.dto.user.UserDto;
import com.Job.Posting.entity.type.StatusType;
import com.fasterxml.jackson.annotation.JsonProperty;
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
    private String coverLetter;

    @JsonProperty("appliedAt")
    private LocalDateTime applied_at;

    @JsonProperty("jobId")
    public Long getJobId() {
        return job != null ? job.getId() : null;
    }

    @JsonProperty("jobTitle")
    public String getJobTitle() {
        return job != null ? job.getTitle() : null;
    }

    @JsonProperty("applicantId")
    public Long getApplicantId() {
        return user != null ? user.getId() : null;
    }

    @JsonProperty("applicantName")
    public String getApplicantName() {
        return user != null ? user.getName() : null;
    }

    @JsonProperty("applicantNumber")
    public String getApplicantNumber() {
        return user != null ? user.getNumber() : null;
    }

    @JsonProperty("applicantLocation")
    public String getApplicantLocation() {
        return user != null ? user.getLocation() : null;
    }

    @JsonProperty("applicantExperience")
    public Integer getApplicantExperience() {
        return user != null ? user.getExperience() : null;
    }
}
