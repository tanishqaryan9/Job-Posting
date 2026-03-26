package com.Job.Posting.job.service.impl;

import com.Job.Posting.dto.job.AddJobRequestDto;
import com.Job.Posting.dto.job.JobDto;
import com.Job.Posting.dto.skills.SkillsDto;
import com.Job.Posting.entity.Job;
import com.Job.Posting.entity.Skills;
import com.Job.Posting.entity.User;
import com.Job.Posting.entity.type.JobType;
import com.Job.Posting.exception.ResourceNotFoundException;
import com.Job.Posting.job.repository.JobRepository;
import com.Job.Posting.job.service.JobService;
import com.Job.Posting.skills.repository.SkillRepository;
import com.Job.Posting.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

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
        return jobRepository.findAll(pageable)
                .map(job -> modelMapper.map(job, JobDto.class));
    }

    @Override
    @Transactional
    public JobDto getJobById(Long id) {
        Job job=jobRepository.findById(id).orElseThrow(()-> new ResourceNotFoundException("Job not found with id: "+id));
        return modelMapper.map(job,JobDto.class);
    }

    @Override
    @Transactional
    public JobDto addNewJob(AddJobRequestDto addJobRequestDto) {
        User user = userRepository.findById(addJobRequestDto.getCreatedByUserId()).orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + addJobRequestDto.getCreatedByUserId()));

        Job job = new Job();
        job.setTitle(addJobRequestDto.getTitle());
        job.setDescription(addJobRequestDto.getDescription());
        job.setSalary(addJobRequestDto.getSalary());
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
    public JobDto updateJob(AddJobRequestDto addJobRequestDto, Long id) {
        Job job = jobRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Job not found with id: " + id));
        User user = userRepository.findById(addJobRequestDto.getCreatedByUserId()).orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + addJobRequestDto.getCreatedByUserId()));

        job.setTitle(addJobRequestDto.getTitle());
        job.setDescription(addJobRequestDto.getDescription());
        job.setSalary(addJobRequestDto.getSalary());
        job.setLocation(addJobRequestDto.getLocation());
        job.setJob_type(addJobRequestDto.getJob_type());
        job.setExperience_required(addJobRequestDto.getExperience_required());
        job.setLatitude(addJobRequestDto.getLatitude());
        job.setLongitude(addJobRequestDto.getLongitude());
        job.setCreatedBy(user);

        Job newJob=jobRepository.save(job);
        return modelMapper.map(newJob, JobDto.class);
    }

    @Override
    @Transactional
    public JobDto updateJobValue(Map<String, Object> updates, Long id) {
        Job job=jobRepository.findById(id).orElseThrow(()-> new ResourceNotFoundException("Job not found with id: "+id));
        updates.forEach((key,value)->
        {
            switch (key)
            {
                case "title": job.setTitle((String)value);
                    break;
                case "description": job.setDescription((String)value);
                    break;
                case "salary": job.setSalary(((Number)value).doubleValue());
                    break;
                case "location": job.setLocation((String)value);
                    break;
                case "job_type": job.setJob_type(JobType.valueOf((String)value));
                    break;
                case "experience_required": job.setExperience_required(((Number) value).intValue());
                    break;
                case "latitude": job.setLatitude(((Number)value).doubleValue());
                    break;
                case "longitude": job.setLongitude(((Number)value).doubleValue());
                    break;
                default:
                    throw new ResourceNotFoundException("Invalid Field!");
            }
        });
        return modelMapper.map(job,JobDto.class);
    }

    @Override
    @Transactional
    public void deleteJob(Long id) {
        if(!jobRepository.existsById(id))
        {
            throw new ResourceNotFoundException("Job not found with id: "+id);
        }
        jobRepository.deleteById(id);
    }

    @Override
    @Transactional
    public List<SkillsDto> getJobSkills(Long id){
        Job job=jobRepository.findById(id).orElseThrow(()-> new ResourceNotFoundException("Job not found with id: "+id));
        return job.getRequiredSkills().stream().map(element -> modelMapper.map(element, SkillsDto.class)).toList();
    }

    @Override
    @Transactional
    public JobDto addSkillsToJob(Long jobId, Long skillId) {
        Job job=jobRepository.findById(jobId).orElseThrow(()-> new ResourceNotFoundException("Job not found with id: "+jobId));
        Skills skills = skillRepository.findById(skillId).orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + skillId));
        job.getRequiredSkills().add(skills);
        return modelMapper.map(job,JobDto.class);
    }

    @Override
    @Transactional
    public void removeSkillFromJob(Long jobId, Long skillId) {
        Job job=jobRepository.findById(jobId).orElseThrow(()-> new ResourceNotFoundException("Job not found with id: "+jobId));
        Skills skills = skillRepository.findById(skillId).orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + skillId));
        job.getRequiredSkills().remove(skills);
    }
}
