package com.Job.Posting;

import com.Job.Posting.dto.job.AddJobRequestDto;
import com.Job.Posting.dto.job.JobDto;
import com.Job.Posting.entity.Job;
import com.Job.Posting.entity.User;
import com.Job.Posting.entity.type.JobType;
import com.Job.Posting.exception.ResourceNotFoundException;
import com.Job.Posting.job.repository.JobRepository;
import com.Job.Posting.job.service.impl.JobServiceImpl;
import com.Job.Posting.skills.repository.SkillRepository;
import com.Job.Posting.user.repository.UserRepository;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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
    }

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

    @Test
    void getAllJobs_shouldReturnPagedResults() {
        Page<Job> jobPage = new PageImpl<>(List.of(job));
        when(jobRepository.findAll(any(PageRequest.class))).thenReturn(jobPage);
        when(modelMapper.map(job, JobDto.class)).thenReturn(jobDto);

        Page<JobDto> result = jobService.getAllJobs(0, 10);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Backend Developer");
    }

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

    @Test
    void deleteJob_shouldDelete_whenExists() {
        when(jobRepository.existsById(1L)).thenReturn(true);

        jobService.deleteJob(1L);

        verify(jobRepository).deleteById(1L);
    }

    @Test
    void deleteJob_shouldThrow_whenNotFound() {
        when(jobRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> jobService.deleteJob(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");

        verify(jobRepository, never()).deleteById(any());
    }

    @Test
    void updateJob_shouldThrow_whenJobNotFound() {
        AddJobRequestDto requestDto = new AddJobRequestDto();
        requestDto.setCreatedByUserId(1L);

        when(jobRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobService.updateJob(requestDto, 99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }
}
