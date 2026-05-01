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
import com.Job.Posting.exception.UserNotVerifiedException;
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
    public List<ApplicationDto> getApplicationsByJob(Long jobId) {
        return jobApplicationRepository.findByJobId(jobId)
                .stream()
                .map(app -> modelMapper.map(app, ApplicationDto.class))
                .toList();
    }

    @Override
    @Transactional
    public List<ApplicationDto> getApplicationsByUser(Long userId) {
        requireProfileMatch(userId);
        return jobApplicationRepository.findByUserId(userId)
                .stream()
                .map(app -> modelMapper.map(app, ApplicationDto.class))
                .toList();
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "application", allEntries = true)
    public ApplicationDto createApplication(AddApplicationDto addApplicationDto) {
        AppUser currentUser = getCurrentUser();
        User proxyUser = currentUser.getUserProfile();

        if (proxyUser == null) {
            throw new ResourceNotFoundException("User profile not found for authenticated user");
        }

        User user = userRepository.findById(proxyUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User profile not found"));

        if (user.getIsVerified() == null || !user.getIsVerified()) {
            throw new UserNotVerifiedException("You must verify your account before applying for a job");
        }

        Job job = jobRepository.findById(addApplicationDto.getJobId())
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with id: " + addApplicationDto.getJobId()));

        if (jobApplicationRepository.existsByJobIdAndUserId(job.getId(), user.getId())) {
            throw new DuplicateResourceException(
                    "User " + user.getId() + " has already applied for job " + job.getId());
        }

        JobApplication jobApplication = new JobApplication();
        jobApplication.setJob(job);
        jobApplication.setUser(user);
        jobApplication.setStatus(StatusType.PENDING);
        jobApplication.setCoverLetter(addApplicationDto.getCoverLetter());
        JobApplication saved = jobApplicationRepository.save(jobApplication);

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

        requireProfileMatch(jobApplication.getUser().getId());

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

        requireProfileMatch(jobApplication.getUser().getId());

        jobApplicationRepository.deleteById(id);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────
    private void requireProfileMatch(Long expectedProfileId) {
        AppUser currentUser = getCurrentUser();        if ("ROLE_ADMIN".equals(currentUser.getRole())) {
            return;
        }        Long currentProfileId = currentUser.getUserProfile() != null
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
