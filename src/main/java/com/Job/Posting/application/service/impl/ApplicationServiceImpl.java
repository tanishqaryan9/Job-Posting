package com.Job.Posting.application.service.impl;

import com.Job.Posting.application.repository.JobApplicationRepository;
import com.Job.Posting.application.service.ApplicationService;
import com.Job.Posting.dto.application.AddApplicationDto;
import com.Job.Posting.dto.application.ApplicationDto;
import com.Job.Posting.entity.Job;
import com.Job.Posting.entity.JobApplication;
import com.Job.Posting.entity.User;
import com.Job.Posting.entity.type.StatusType;
import com.Job.Posting.exception.ResourceNotFoundException;
import com.Job.Posting.job.repository.JobRepository;
import com.Job.Posting.notification.service.NotificationService;
import com.Job.Posting.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ApplicationServiceImpl implements ApplicationService {

    private final JobApplicationRepository jobApplicationRepository;
    private final ModelMapper modelMapper;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional
    @Cacheable(cacheNames = "application", key = "all")
    public List<ApplicationDto> getAllApplications() {
        List<JobApplication> jobApplications=jobApplicationRepository.findAll();
        return jobApplications.stream().
        map(application-> modelMapper.map(application,ApplicationDto.class)).toList();
    }

    @Override
    @Transactional
    @Cacheable(cacheNames = "application", key = "#id")
    public ApplicationDto getApplicationByID(Long id) {
        JobApplication jobApplication=jobApplicationRepository.findById(id).orElseThrow(()-> new ResourceNotFoundException("Application not found with id: "+id));
        return modelMapper.map(jobApplication,ApplicationDto.class);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "application", allEntries = true)
    public ApplicationDto createApplication(AddApplicationDto addApplicationDto) {
        Job job = jobRepository.findById(addApplicationDto.getJobId()).orElseThrow(() -> new ResourceNotFoundException("Job not found with id: " + addApplicationDto.getJobId()));
        User user = userRepository.findById(addApplicationDto.getUserId()).orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + addApplicationDto.getUserId()));

        JobApplication jobApplication = new JobApplication();
        jobApplication.setJob(job);
        jobApplication.setUser(user);
        jobApplication.setStatus(StatusType.PENDING);

        JobApplication saved = jobApplicationRepository.save(jobApplication);

        //notifying user that application was submitted
        notificationService.sendNotification(
                user,
                "Application Submitted",
                "Your application for "+job.getTitle()+" has been submitted successfully!"
        );

        //notify employer that someone applied to their job
        notificationService.sendNotification(
                job.getCreatedBy(),
                "New Application",
                user.getName()+" has applied for your job "+job.getTitle()
        );

        return modelMapper.map(saved, ApplicationDto.class);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "application", allEntries = true)
    public ApplicationDto updateApplication(AddApplicationDto addApplicationDto, Long id) {
        JobApplication jobApplication = jobApplicationRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Application not found with id: " + id));
        Job job = jobRepository.findById(addApplicationDto.getJobId()).orElseThrow(() -> new ResourceNotFoundException("Job not found with id: " + addApplicationDto.getJobId()));
        User user = userRepository.findById(addApplicationDto.getUserId()).orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + addApplicationDto.getUserId()));

        jobApplication.setJob(job);
        jobApplication.setUser(user);
        return modelMapper.map(jobApplication, ApplicationDto.class);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "application", allEntries = true)
    public ApplicationDto updateApplicationValue(Map<String, Object> update, Long id) {
        JobApplication jobApplication=jobApplicationRepository.findById(id).orElseThrow(()-> new ResourceNotFoundException("Application not found with id: "+id));
        update.forEach((key,value)->{
            if(key.equals("status"))
            {
                StatusType newStatus = StatusType.valueOf((String) value);
                jobApplication.setStatus(newStatus);

                //notify worker when status changes
                String notificationBody = getNotificationBody(newStatus, jobApplication.getJob().getTitle());
                notificationService.sendNotification(
                        jobApplication.getUser(),
                        "Application Update",
                        notificationBody);
            }
        });
        jobApplicationRepository.save(jobApplication);
        return modelMapper.map(jobApplication,ApplicationDto.class);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "application", allEntries = true)
    public void deleteApplication(Long id) {
        if(!jobApplicationRepository.existsById(id))
        {
            throw new ResourceNotFoundException("Application not found with id: "+id);
        }
        jobApplicationRepository.deleteById(id);
    }

    //builds notification message based on status
    private String getNotificationBody(StatusType status, String jobTitle) {
        return switch (status) {
            case SHORTLISTED -> "Congratulations! You have been shortlisted for " + jobTitle;
            case HIRED -> "Great news! You have been hired for " + jobTitle;
            case REJECTED -> "Unfortunately your application for " + jobTitle + " was not selected";
            case PENDING -> "Your application for " + jobTitle + " is under review";
            default -> "Your application status for " + jobTitle + " has been updated to " + status;
        };
    }
}
