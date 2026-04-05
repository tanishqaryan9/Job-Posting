package com.Job.Posting.application.service.impl;

import com.Job.Posting.application.repository.JobApplicationRepository;
import com.Job.Posting.application.service.ApplicationService;
import com.Job.Posting.dto.kafka.ApplicationSubmittedEvent;
import com.Job.Posting.dto.application.AddApplicationDto;
import com.Job.Posting.dto.application.ApplicationDto;
import com.Job.Posting.entity.AppUser;
import com.Job.Posting.entity.Job;
import com.Job.Posting.entity.JobApplication;
import com.Job.Posting.entity.User;
import com.Job.Posting.entity.type.StatusType;
import com.Job.Posting.exception.AccessDeniedException;
import com.Job.Posting.exception.DuplicateResourceException;
import com.Job.Posting.exception.ResourceNotFoundException;
import com.Job.Posting.job.repository.JobRepository;
import com.Job.Posting.notification.service.NotificationService;
import com.Job.Posting.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public Page<ApplicationDto> getAllApplications(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Long> idPage = jobApplicationRepository.findAllIds(pageable);
        List<ApplicationDto> dtos = jobApplicationRepository.findAllByIds(idPage.getContent())
                .stream()
                .map(app -> modelMapper.map(app, ApplicationDto.class))
                .toList();
        return new PageImpl<>(dtos, pageable, idPage.getTotalElements());
    }

    @Override
    @Transactional
    @Cacheable(cacheNames = "application", key = "#id")
    public ApplicationDto getApplicationByID(Long id) {
        JobApplication jobApplication = jobApplicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with id: " + id));
        return modelMapper.map(jobApplication, ApplicationDto.class);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "application", allEntries = true)
    public ApplicationDto createApplication(AddApplicationDto addApplicationDto) {
        Job job = jobRepository.findById(addApplicationDto.getJobId())
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with id: " + addApplicationDto.getJobId()));
        User user = userRepository.findById(addApplicationDto.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + addApplicationDto.getUserId()));

        // Only the authenticated user may apply on their own behalf
        requireProfileMatch(user.getId());

        if (jobApplicationRepository.existsByJobIdAndUserId(job.getId(), user.getId())) {
            throw new DuplicateResourceException(
                    "User " + user.getId() + " has already applied for job " + job.getId());
        }

        JobApplication jobApplication = new JobApplication();
        jobApplication.setJob(job);
        jobApplication.setUser(user);
        jobApplication.setStatus(StatusType.PENDING);
        JobApplication saved = jobApplicationRepository.save(jobApplication);

        // Publish Spring domain event — @TransactionalEventListener publishes to Kafka after commit
        ApplicationSubmittedEvent event = new ApplicationSubmittedEvent();
        event.setApplicationId(saved.getId());
        event.setUserId(user.getId());
        event.setJobId(job.getId());
        event.setJobTitle(job.getTitle());
        event.setUserName(user.getName());
        event.setUserFcmToken(user.getFcmToken());
        event.setJobCreatorId(job.getCreatedBy().getId());
        event.setJobCreatorFcmToken(job.getCreatedBy().getFcmToken());

        eventPublisher.publishEvent(event);

        return modelMapper.map(saved, ApplicationDto.class);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "application", allEntries = true)
    public ApplicationDto updateApplication(AddApplicationDto addApplicationDto, Long id) {
        JobApplication jobApplication = jobApplicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with id: " + id));

        // Only the applicant may update their own application
        requireProfileMatch(jobApplication.getUser().getId());

        // PUT is intentionally restricted — job and user identity fields are immutable
        // after creation. Status changes go through PATCH (employer-only).
        // Only the status field is updated here so applicants can withdraw via PENDING.
        if (addApplicationDto.getStatus() != null) {
            jobApplication.setStatus(addApplicationDto.getStatus());
        }

        jobApplicationRepository.save(jobApplication);
        return modelMapper.map(jobApplication, ApplicationDto.class);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "application", allEntries = true)
    public ApplicationDto updateApplicationValue(Map<String, Object> update, Long id) {
        JobApplication jobApplication = jobApplicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with id: " + id));

        // Status updates are performed by the job creator (employer), not the applicant
        requireProfileMatch(jobApplication.getJob().getCreatedBy().getId());

        update.forEach((key, value) -> {
            if (key.equals("status")) {
                StatusType newStatus = StatusType.valueOf((String) value);
                jobApplication.setStatus(newStatus);
                notificationService.sendNotification(
                        jobApplication.getUser(),
                        "Application Update",
                        getNotificationBody(newStatus, jobApplication.getJob().getTitle()));
            }
        });

        jobApplicationRepository.save(jobApplication);
        return modelMapper.map(jobApplication, ApplicationDto.class);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "application", allEntries = true)
    public void deleteApplication(Long id) {
        JobApplication jobApplication = jobApplicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with id: " + id));

        // Only the applicant may withdraw their own application
        requireProfileMatch(jobApplication.getUser().getId());

        jobApplicationRepository.deleteById(id);
    }

    //Helpers
    private void requireProfileMatch(Long expectedProfileId) {
        AppUser currentUser = getCurrentUser();
        Long currentProfileId = currentUser.getUserProfile() != null
                ? currentUser.getUserProfile().getId() : null;

        if (currentProfileId == null || !currentProfileId.equals(expectedProfileId)) {
            throw new AccessDeniedException("You are not authorised to perform this action");
        }
    }

    private AppUser getCurrentUser() {
        return (AppUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private String getNotificationBody(StatusType status, String jobTitle) {
        return switch (status) {
            case SHORTLISTED -> "Congratulations! You have been shortlisted for " + jobTitle;
            case HIRED       -> "Great news! You have been hired for " + jobTitle;
            case REJECTED    -> "Unfortunately your application for " + jobTitle + " was not selected";
            case PENDING     -> "Your application for " + jobTitle + " is under review";
            default          -> "Your application status for " + jobTitle + " has been updated to " + status;
        };
    }
}