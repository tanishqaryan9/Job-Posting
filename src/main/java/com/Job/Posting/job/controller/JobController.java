package com.Job.Posting.job.controller;

import com.Job.Posting.dto.job.AddJobRequestDto;
import com.Job.Posting.dto.job.JobDto;
import com.Job.Posting.dto.skills.SkillsDto;
import com.Job.Posting.job.service.JobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/jobs")
public class JobController {

    private final JobService jobService;

    @GetMapping
    public ResponseEntity<Page<JobDto>> getAllJobs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(jobService.getAllJobs(page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobDto> getJobById(@PathVariable Long id)
    {
        return ResponseEntity.ok(jobService.getJobById(id));
    }

    @PostMapping
    public ResponseEntity<JobDto> addNewJob(@RequestBody @Valid AddJobRequestDto addJobRequestDto)
    {
        return ResponseEntity.status(HttpStatus.CREATED).body(jobService.addNewJob(addJobRequestDto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<JobDto> updateJob(@RequestBody @Valid AddJobRequestDto addJobRequestDto, @PathVariable Long id)
    {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(jobService.updateJob(addJobRequestDto,id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<JobDto> updateJobValue(@RequestBody Map<String,Object> updates, @PathVariable Long id)
    {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(jobService.updateJobValue(updates,id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteJob(@PathVariable Long id)
    {
        jobService.deleteJob(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{jobId}/skills")
    public ResponseEntity<List<SkillsDto>> getJobSkills(@PathVariable Long jobId)
    {
        return ResponseEntity.ok(jobService.getJobSkills(jobId));
    }

    @PostMapping("/{jobId}/skills/{skillId}")
    public ResponseEntity<JobDto> addSkillsToJob(@PathVariable Long jobId, @PathVariable Long skillId)
    {
        return ResponseEntity.status(HttpStatus.CREATED).body(jobService.addSkillsToJob(jobId,skillId));
    }

    @DeleteMapping("/{jobId}/skills/{skillId}")
    public ResponseEntity<Void> removeSkillFromJob(@PathVariable Long jobId, @PathVariable Long skillId)
    {
        jobService.removeSkillFromJob(jobId,skillId);
        return ResponseEntity.noContent().build();
    }

}
