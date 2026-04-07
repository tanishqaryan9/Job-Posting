package com.Job.Posting.dto.job;

import com.Job.Posting.dto.skills.SkillsDto;
import com.Job.Posting.entity.type.JobType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
    private String location;
    private JobType jobType;
    private Integer experienceRequired;
    private CreatedByDto createdBy;
    private LocalDateTime createdAt;
    private Set<SkillsDto> skills;
    private Double latitude;
    private Double longitude;
    private Double distanceKm;
    private Integer skillMatchScore;
}
