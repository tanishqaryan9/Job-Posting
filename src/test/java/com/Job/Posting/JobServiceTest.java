package com.Job.Posting;

import com.Job.Posting.dto.job.AddJobRequestDto;
import com.Job.Posting.dto.job.JobDto;
import com.Job.Posting.entity.AppUser;
import com.Job.Posting.entity.Job;
import com.Job.Posting.entity.User;
import com.Job.Posting.entity.type.AuthProviderType;
import com.Job.Posting.entity.type.JobType;
import com.Job.Posting.exception.ResourceNotFoundException;
import com.Job.Posting.job.repository.JobRepository;
import com.Job.Posting.job.service.impl.JobServiceImpl;
import com.Job.Posting.skills.repository.SkillRepository;
import com.Job.Posting.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobServiceTest {

    @Mock private JobRepository jobRepository;
    @Mock private UserRepository userRepository;
    @Mock private SkillRepository skillRepository;
    @Mock private ModelMapper modelMapper;

    @InjectMocks private JobServiceImpl jobService;

    private Job job;
    private JobDto jobDto;
    private User user;
    private AppUser appUser;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setName("Test User");
        user.setNumber("1234567890");
        user.setLocation("Delhi");
        user.setExperience(2);

        job = new Job();
        job.setId(1L);
        job.setTitle("Backend Developer");
        job.setDescription("Spring Boot role");
        job.setSalary(80000.0);
        job.setLocation("Delhi");
        job.setJob_type(JobType.FULL_TIME);
        job.setExperience_required(2);
        job.setCreatedBy(user);

        jobDto = new JobDto();
        jobDto.setId(1L);
        jobDto.setTitle("Backend Developer");
        jobDto.setSalary(80000.0);

        // AppUser whose profile is 'user' (id=1) — satisfies requireJobOwnership
        appUser = AppUser.builder()
                .id(10L)
                .username("test@example.com")
                .password("encoded")
                .providerType(AuthProviderType.EMAIL)
                .userProfile(user)
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(appUser, null, appUser.getAuthorities())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ── getJobById ────────────────────────────────────────────────────────────

    @Test
    void getJobById_shouldReturnJob_whenExists() {
        when(jobRepository.findById(1L)).thenReturn(Optional.of(job));
        when(modelMapper.map(job, JobDto.class)).thenReturn(jobDto);

        JobDto result = jobService.getJobById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("Backend Developer");
        verify(jobRepository).findById(1L);
    }

    @Test
    void getJobById_shouldThrow_whenNotFound() {
        when(jobRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobService.getJobById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // ── getAllJobs ────────────────────────────────────────────────────────────

    @Test
    void getAllJobs_shouldReturnPagedResults() {
        Page<Job> jobPage = new PageImpl<>(List.of(job));
        // SecurityContext is populated with appUser whose profile id = 1,
        // so the impl calls findAllExcludingCreator, not findAll.
        when(jobRepository.findAllExcludingCreator(eq(1L), any(PageRequest.class))).thenReturn(jobPage);
        when(modelMapper.map(job, JobDto.class)).thenReturn(jobDto);

        Page<JobDto> result = jobService.getAllJobs(0, 10);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Backend Developer");
    }

    // ── addNewJob ─────────────────────────────────────────────────────────────
    // addNewJob does NOT call requireJobOwnership — no SecurityContext manipulation needed

    @Test
    void addNewJob_shouldSaveAndReturnJob() {
        AddJobRequestDto requestDto = new AddJobRequestDto();
        requestDto.setTitle("Backend Developer");
        requestDto.setDescription("Spring Boot role");
        requestDto.setSalary(80000.0);
        requestDto.setLocation("Delhi");
        requestDto.setJob_type(JobType.FULL_TIME);
        requestDto.setExperience_required(2);
        requestDto.setCreatedByUserId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(jobRepository.save(any(Job.class))).thenReturn(job);
        when(modelMapper.map(job, JobDto.class)).thenReturn(jobDto);

        JobDto result = jobService.addNewJob(requestDto);

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Backend Developer");
        verify(jobRepository).save(any(Job.class));
    }

    @Test
    void addNewJob_shouldThrow_whenUserNotFound() {
        AddJobRequestDto requestDto = new AddJobRequestDto();
        requestDto.setCreatedByUserId(99L);
        requestDto.setTitle("Test");

        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobService.addNewJob(requestDto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // ── deleteJob ─────────────────────────────────────────────────────────────

    @Test
    void deleteJob_shouldSoftDelete_whenExists() {
        when(jobRepository.findById(1L)).thenReturn(Optional.of(job));

        jobService.deleteJob(1L);

        // Soft delete: sets deleted_at and calls save, never deleteById
        verify(jobRepository).save(any(Job.class));
        verify(jobRepository, never()).deleteById(any());
    }

    @Test
    void deleteJob_shouldThrow_whenNotFound() {
        when(jobRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobService.deleteJob(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");

        verify(jobRepository, never()).save(any());
    }

    // ── updateJob ─────────────────────────────────────────────────────────────

    @Test
    void updateJob_shouldThrow_whenJobNotFound() {
        AddJobRequestDto requestDto = new AddJobRequestDto();
        requestDto.setCreatedByUserId(1L);

        when(jobRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobService.updateJob(requestDto, 99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void updateJob_shouldSaveUpdatedJob_whenOwner() {
        AddJobRequestDto requestDto = new AddJobRequestDto();
        requestDto.setTitle("Updated Title");
        requestDto.setDescription("Updated Desc");
        requestDto.setSalary(90000.0);
        requestDto.setLocation("Mumbai");
        requestDto.setJob_type(JobType.PART_TIME);
        requestDto.setExperience_required(3);
        requestDto.setCreatedByUserId(1L);

        when(jobRepository.findById(1L)).thenReturn(Optional.of(job));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(jobRepository.save(any(Job.class))).thenReturn(job);
        when(modelMapper.map(job, JobDto.class)).thenReturn(jobDto);

        JobDto result = jobService.updateJob(requestDto, 1L);

        assertThat(result).isNotNull();
        verify(jobRepository).save(any(Job.class));
    }
}