package com.Job.Posting.dto.job;

import com.Job.Posting.dto.skills.SkillsDto;
import com.Job.Posting.entity.type.JobType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class JobDto {

    private Long id;
    private String title;
    private String description;
    private Double salary;

    private String salaryPeriod;

    private String location;

    @JsonProperty("jobType")
    private JobType job_type;

    @JsonProperty("experienceRequired")
    private Integer experience_required;

    private CreatedByDto createdBy;

    @JsonProperty("createdAt")
    private LocalDateTime created_at;

    @JsonProperty("skills")
    private Set<SkillsDto> requiredSkills;

    private Double latitude;
    private Double longitude;
    private Double distanceKm;
    private Integer skillMatchScore;
}
