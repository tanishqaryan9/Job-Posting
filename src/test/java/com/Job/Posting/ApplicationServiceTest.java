package com.Job.Posting;

import com.Job.Posting.application.repository.JobApplicationRepository;
import com.Job.Posting.application.service.impl.ApplicationServiceImpl;
import com.Job.Posting.dto.application.AddApplicationDto;
import com.Job.Posting.dto.application.ApplicationDto;
import com.Job.Posting.dto.kafka.ApplicationSubmittedEvent;
import com.Job.Posting.entity.AppUser;
import com.Job.Posting.entity.Job;
import com.Job.Posting.entity.JobApplication;
import com.Job.Posting.entity.User;
import com.Job.Posting.entity.type.AuthProviderType;
import com.Job.Posting.entity.type.StatusType;
import com.Job.Posting.exception.DuplicateResourceException;
import com.Job.Posting.exception.ResourceNotFoundException;
import com.Job.Posting.job.repository.JobRepository;
import com.Job.Posting.notification.service.NotificationService;
import com.Job.Posting.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock private JobApplicationRepository jobApplicationRepository;
    @Mock private JobRepository jobRepository;
    @Mock private UserRepository userRepository;
    @Mock private NotificationService notificationService;
    @Mock private ModelMapper modelMapper;
    @Mock private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks private ApplicationServiceImpl applicationService;

    private User user;
    private User employer;
    private Job job;
    private JobApplication jobApplication;
    private ApplicationDto applicationDto;
    private AppUser applicantAppUser;
    private AppUser employerAppUser;

    @BeforeEach
    void setUp() {
        employer = new User();
        employer.setId(99L);
        employer.setName("Employer");

        user = new User();
        user.setId(1L);
        user.setName("John Doe");

        job = new Job();
        job.setId(10L);
        job.setTitle("Backend Developer");
        job.setCreatedBy(employer);

        jobApplication = new JobApplication();
        jobApplication.setId(100L);
        jobApplication.setJob(job);
        jobApplication.setUser(user);
        jobApplication.setStatus(StatusType.PENDING);

        applicationDto = new ApplicationDto();
        applicationDto.setId(100L);

        // Applicant principal (profile id = 1)
        applicantAppUser = AppUser.builder()
                .id(1L)
                .username("john@example.com")
                .password("encoded")
                .providerType(AuthProviderType.EMAIL)
                .userProfile(user)
                .build();

        // Employer principal (profile id = 99)
        employerAppUser = AppUser.builder()
                .id(2L)
                .username("employer@example.com")
                .password("encoded")
                .providerType(AuthProviderType.EMAIL)
                .userProfile(employer)
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void loginAs(AppUser principal) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    // ── getAllApplications ────────────────────────────────────────────────────
    // Uses two-query pattern: findAllIds(pageable) → findAllByIds(ids)

    @Test
    void getAllApplications_shouldReturnPagedResults() {
        Page<Long> idPage = new PageImpl<>(List.of(100L));
        when(jobApplicationRepository.findAllIds(any(PageRequest.class))).thenReturn(idPage);
        when(jobApplicationRepository.findAllByIds(List.of(100L))).thenReturn(List.of(jobApplication));
        when(modelMapper.map(jobApplication, ApplicationDto.class)).thenReturn(applicationDto);

        Page<ApplicationDto> result = applicationService.getAllApplications(0, 10);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(100L);
    }

    // ── getApplicationByID ────────────────────────────────────────────────────

    @Test
    void getApplicationByID_shouldReturnDto_whenExists() {
        when(jobApplicationRepository.findById(100L)).thenReturn(Optional.of(jobApplication));
        when(modelMapper.map(jobApplication, ApplicationDto.class)).thenReturn(applicationDto);

        ApplicationDto result = applicationService.getApplicationByID(100L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(100L);
    }

    @Test
    void getApplicationByID_shouldThrow_whenNotFound() {
        when(jobApplicationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> applicationService.getApplicationByID(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    // ── createApplication ─────────────────────────────────────────────────────
    // createApplication calls requireProfileMatch(user.getId()) — logged-in user must be the applicant

    @Test
    void createApplication_shouldSaveAndPublishEvent_whenValid() {
        loginAs(applicantAppUser);
        AddApplicationDto dto = new AddApplicationDto(10L, 1L, "resume.pdf", StatusType.PENDING);

        when(jobRepository.findById(10L)).thenReturn(Optional.of(job));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(jobApplicationRepository.existsByJobIdAndUserId(10L, 1L)).thenReturn(false);
        when(jobApplicationRepository.save(any(JobApplication.class))).thenReturn(jobApplication);
        when(modelMapper.map(jobApplication, ApplicationDto.class)).thenReturn(applicationDto);

        ApplicationDto result = applicationService.createApplication(dto);

        assertThat(result).isNotNull();
        verify(jobApplicationRepository).save(any(JobApplication.class));
        // Event is published to Kafka via ApplicationEventPublisher (not direct notifications)
        verify(applicationEventPublisher)
                .publishEvent(any(ApplicationSubmittedEvent.class));
    }

    @Test
    void createApplication_shouldThrow_whenJobNotFound() {
        loginAs(applicantAppUser);
        AddApplicationDto dto = new AddApplicationDto(999L, 1L, "resume.pdf", StatusType.PENDING);
        when(jobRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> applicationService.createApplication(dto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");

        verify(jobApplicationRepository, never()).save(any());
    }

    @Test
    void createApplication_shouldThrow_whenUserNotFound() {
        loginAs(applicantAppUser);
        AddApplicationDto dto = new AddApplicationDto(10L, 999L, "resume.pdf", StatusType.PENDING);
        when(jobRepository.findById(10L)).thenReturn(Optional.of(job));
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> applicationService.createApplication(dto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");

        verify(jobApplicationRepository, never()).save(any());
    }

    @Test
    void createApplication_shouldThrow_whenDuplicateApplication() {
        loginAs(applicantAppUser);
        AddApplicationDto dto = new AddApplicationDto(10L, 1L, "resume.pdf", StatusType.PENDING);
        when(jobRepository.findById(10L)).thenReturn(Optional.of(job));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(jobApplicationRepository.existsByJobIdAndUserId(10L, 1L)).thenReturn(true);

        assertThatThrownBy(() -> applicationService.createApplication(dto))
                .isInstanceOf(DuplicateResourceException.class);

        verify(jobApplicationRepository, never()).save(any());
    }

    // ── updateApplicationValue ────────────────────────────────────────────────
    // updateApplicationValue calls requireProfileMatch(job.getCreatedBy().getId()) — must log in as employer

    @Test
    void updateApplicationValue_shouldUpdateStatus_andNotifyUser() {
        loginAs(employerAppUser); // employer owns the job
        when(jobApplicationRepository.findById(100L)).thenReturn(Optional.of(jobApplication));
        when(jobApplicationRepository.save(jobApplication)).thenReturn(jobApplication);
        when(modelMapper.map(jobApplication, ApplicationDto.class)).thenReturn(applicationDto);

        applicationService.updateApplicationValue(Map.of("status", "HIRED"), 100L);

        assertThat(jobApplication.getStatus()).isEqualTo(StatusType.HIRED);
        verify(notificationService).sendNotification(eq(user), eq("Application Update"), anyString());
    }

    @Test
    void updateApplicationValue_shouldSendShortlistedMessage() {
        loginAs(employerAppUser);
        when(jobApplicationRepository.findById(100L)).thenReturn(Optional.of(jobApplication));
        when(jobApplicationRepository.save(any())).thenReturn(jobApplication);
        when(modelMapper.map(any(), eq(ApplicationDto.class))).thenReturn(applicationDto);

        applicationService.updateApplicationValue(Map.of("status", "SHORTLISTED"), 100L);

        verify(notificationService).sendNotification(eq(user), eq("Application Update"),
                contains("shortlisted"));
    }

    @Test
    void updateApplicationValue_shouldSendRejectedMessage() {
        loginAs(employerAppUser);
        when(jobApplicationRepository.findById(100L)).thenReturn(Optional.of(jobApplication));
        when(jobApplicationRepository.save(any())).thenReturn(jobApplication);
        when(modelMapper.map(any(), eq(ApplicationDto.class))).thenReturn(applicationDto);

        applicationService.updateApplicationValue(Map.of("status", "REJECTED"), 100L);

        verify(notificationService).sendNotification(eq(user), eq("Application Update"),
                contains("not selected"));
    }

    @Test
    void updateApplicationValue_shouldThrow_whenNotFound() {
        loginAs(employerAppUser);
        when(jobApplicationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> applicationService.updateApplicationValue(Map.of("status", "HIRED"), 999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    // ── deleteApplication ─────────────────────────────────────────────────────
    // deleteApplication calls requireProfileMatch(application.getUser().getId()) — must log in as applicant

    @Test
    void deleteApplication_shouldDelete_whenExists() {
        loginAs(applicantAppUser);
        when(jobApplicationRepository.findById(100L)).thenReturn(Optional.of(jobApplication));

        applicationService.deleteApplication(100L);

        verify(jobApplicationRepository).deleteById(100L);
    }

    @Test
    void deleteApplication_shouldThrow_whenNotFound() {
        loginAs(applicantAppUser);
        when(jobApplicationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> applicationService.deleteApplication(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");

        verify(jobApplicationRepository, never()).deleteById(any());
    }
}