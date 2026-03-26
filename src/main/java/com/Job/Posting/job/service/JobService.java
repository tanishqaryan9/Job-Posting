package com.Job.Posting.job.service;

import com.Job.Posting.dto.job.AddJobRequestDto;
import com.Job.Posting.dto.job.JobDto;
import com.Job.Posting.dto.skills.SkillsDto;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public interface JobService {


    Page<JobDto> getAllJobs(int page, int size);

    JobDto getJobById(Long id);

    JobDto addNewJob(AddJobRequestDto addJobRequestDto);

    JobDto updateJob(AddJobRequestDto addJobRequestDto, Long id);

    JobDto updateJobValue(Map<String, Object> updates, Long id);

    void deleteJob(Long id);

    List<SkillsDto> getJobSkills(Long id);

    JobDto addSkillsToJob(Long jobId, Long skillId);

    void removeSkillFromJob(Long jobId, Long skillId);
}
