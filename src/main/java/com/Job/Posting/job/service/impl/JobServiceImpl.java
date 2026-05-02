package com.Job.Posting.job.service.impl;

import com.Job.Posting.dto.job.AddJobRequestDto;
import com.Job.Posting.dto.job.JobDto;
import com.Job.Posting.dto.skills.SkillsDto;
import com.Job.Posting.entity.AppUser;
import com.Job.Posting.entity.Job;
import com.Job.Posting.entity.Skills;
import com.Job.Posting.entity.User;
import com.Job.Posting.entity.type.JobType;
import com.Job.Posting.exception.AccessDeniedException;
import com.Job.Posting.exception.ResourceNotFoundException;
import com.Job.Posting.exception.UserNotVerifiedException;
import com.Job.Posting.job.repository.JobRepository;
import com.Job.Posting.job.service.JobService;
import com.Job.Posting.skills.repository.SkillRepository;
import com.Job.Posting.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class JobServiceImpl implements JobService {

    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final SkillRepository skillRepository;
    private final ModelMapper modelMapper;

    @Override
    @Transactional
    public Page<JobDto> getAllJobs(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        try {
            AppUser currentUser = getCurrentUser();
            Long profileId = currentUser.getUserProfile() != null
                    ? currentUser.getUserProfile().getId() : null;
            if (profileId != null) {
                return jobRepository.findAllExcludingCreator(profileId, pageable)
                        .map(job -> modelMapper.map(job, JobDto.class));
            }
        } catch (Exception ignored) {
            // If security context is unavailable (e.g. in tests), fall back to all jobs
        }

        return jobRepository.findAll(pageable).map(job -> modelMapper.map(job, JobDto.class));
    }

    @Override
    @Transactional
    @Cacheable(value = "jobs", key = "#id")
    public JobDto getJobById(Long id) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with id: " + id));
        return modelMapper.map(job, JobDto.class);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"jobs", "feed"}, allEntries = true)
    public JobDto addNewJob(AddJobRequestDto addJobRequestDto) {
        AppUser currentUser = getCurrentUser();
        User userProxy = currentUser.getUserProfile();

        if (userProxy == null) {
            throw new ResourceNotFoundException("User profile not found for authenticated user");
        }
        
        User user = userRepository.findById(userProxy.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User profile not found"));

        if (user.getIsVerified() == null || !user.getIsVerified()) {
            throw new UserNotVerifiedException("You must verify your account before posting a job");
        }

        Job job = new Job();
        job.setTitle(addJobRequestDto.getTitle());
        job.setDescription(addJobRequestDto.getDescription());
        job.setSalary(addJobRequestDto.getSalary());
        job.setSalaryPeriod(addJobRequestDto.getSalaryPeriod());
        job.setLocation(addJobRequestDto.getLocation());
        job.setJob_type(addJobRequestDto.getJob_type());
        job.setExperience_required(addJobRequestDto.getExperience_required());
        job.setLatitude(addJobRequestDto.getLatitude());
        job.setLongitude(addJobRequestDto.getLongitude());
        job.setCreatedBy(user);

        job = jobRepository.save(job);
        return modelMapper.map(job, JobDto.class);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "jobs", key = "#id"),
            @CacheEvict(value = "jobs", allEntries = true),
            @CacheEvict(value = "feed", allEntries = true)
    })
    public JobDto updateJob(AddJobRequestDto addJobRequestDto, Long id) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with id: " + id));
        requireJobOwnership(job);

        job.setTitle(addJobRequestDto.getTitle());
        job.setDescription(addJobRequestDto.getDescription());
        job.setSalary(addJobRequestDto.getSalary());
        job.setSalaryPeriod(addJobRequestDto.getSalaryPeriod());
        job.setLocation(addJobRequestDto.getLocation());
        job.setJob_type(addJobRequestDto.getJob_type());
        job.setExperience_required(addJobRequestDto.getExperience_required());
        job.setLatitude(addJobRequestDto.getLatitude());
        job.setLongitude(addJobRequestDto.getLongitude());

        Job newJob = jobRepository.save(job);
        return modelMapper.map(newJob, JobDto.class);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "jobs", key = "#id"),
            @CacheEvict(value = "jobs", allEntries = true),
            @CacheEvict(value = "feed", allEntries = true)
    })
    public JobDto updateJobValue(Map<String, Object> updates, Long id) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with id: " + id));
        requireJobOwnership(job);

        updates.forEach((key, value) -> {
            switch (key) {
                case "title"               -> job.setTitle((String) value);
                case "description"         -> job.setDescription((String) value);
                case "salary"              -> job.setSalary(((Number) value).doubleValue());
                case "salaryPeriod"        -> job.setSalaryPeriod((String) value);
                case "location"            -> job.setLocation((String) value);
                case "job_type"            -> job.setJob_type(JobType.valueOf((String) value));
                case "experience_required" -> job.setExperience_required(((Number) value).intValue());
                case "latitude"            -> job.setLatitude(((Number) value).doubleValue());
                case "longitude"           -> job.setLongitude(((Number) value).doubleValue());
                default -> throw new ResourceNotFoundException("Invalid field: " + key);
            }
        });
        Job saved = jobRepository.save(job);
        return modelMapper.map(saved, JobDto.class);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "jobs", key = "#id"),
            @CacheEvict(value = "jobs", allEntries = true),
            @CacheEvict(value = "feed", allEntries = true)
    })
    public void deleteJob(Long id) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with id: " + id));
        requireJobOwnership(job);
        job.setDeleted_at(LocalDateTime.now());
        jobRepository.save(job);
    }

    @Override
    @Transactional
    public List<JobDto> getJobsByCreator(Long userId) {
        return jobRepository.findByCreatedById(userId)
                .stream()
                .map(job -> modelMapper.map(job, JobDto.class))
                .toList();
    }

    @Override
    @Transactional
    public List<SkillsDto> getJobSkills(Long id) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with id: " + id));
        return job.getRequiredSkills().stream()
                .map(element -> modelMapper.map(element, SkillsDto.class))
                .toList();
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "jobs", key = "#jobId"),
            @CacheEvict(value = "feed", allEntries = true)
    })
    public JobDto addSkillsToJob(Long jobId, Long skillId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with id: " + jobId));
        requireJobOwnership(job);
        Skills skills = skillRepository.findById(skillId)
                .orElseThrow(() -> new ResourceNotFoundException("Skill not found with id: " + skillId));
        job.getRequiredSkills().add(skills);
        return modelMapper.map(job, JobDto.class);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "jobs", key = "#jobId"),
            @CacheEvict(value = "feed", allEntries = true)
    })
    public void removeSkillFromJob(Long jobId, Long skillId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with id: " + jobId));
        requireJobOwnership(job);
        Skills skills = skillRepository.findById(skillId)
                .orElseThrow(() -> new ResourceNotFoundException("Skill not found with id: " + skillId));
        job.getRequiredSkills().remove(skills);
    }

    private void requireJobOwnership(Job job) {
        AppUser currentUser = getCurrentUser();
        if ("ROLE_ADMIN".equals(currentUser.getRole())) {
            return;
        }
        Long currentProfileId = currentUser.getUserProfile() != null
                ? currentUser.getUserProfile().getId() : null;
        Long creatorId = job.getCreatedBy() != null ? job.getCreatedBy().getId() : null;
        if (currentProfileId == null || !currentProfileId.equals(creatorId)) {
            throw new AccessDeniedException("You are not allowed to modify this job");
        }
    }

    private AppUser getCurrentUser() {
        return (AppUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}