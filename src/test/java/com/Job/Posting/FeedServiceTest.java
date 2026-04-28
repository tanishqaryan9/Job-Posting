package com.Job.Posting;

import com.Job.Posting.application.repository.JobApplicationRepository;
import com.Job.Posting.dto.job.JobDto;
import com.Job.Posting.entity.Job;
import com.Job.Posting.entity.Skills;
import com.Job.Posting.entity.User;
import com.Job.Posting.exception.ResourceNotFoundException;
import com.Job.Posting.feed.service.impl.FeedServiceImpl;
import com.Job.Posting.job.repository.JobRepository;
import com.Job.Posting.user.repository.UserRepository;
import com.Job.Posting.util.HaversineUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Pageable;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeedServiceTest {

    @Mock private JobRepository jobRepository;
    @Mock private UserRepository userRepository;
    @Mock private JobApplicationRepository jobApplicationRepository;
    @Mock private HaversineUtil haversineUtil;
    @Mock private ModelMapper modelMapper;

    @InjectMocks private FeedServiceImpl feedService;

    private User user;
    private Job job1;
    private Job job2;
    private JobDto jobDto1;
    private JobDto jobDto2;
    private Skills java;
    private Skills python;

    @BeforeEach
    void setUp() {
        java = new Skills();
        java.setId(1L);
        java.setName("Java");

        python = new Skills();
        python.setId(2L);
        python.setName("Python");

        user = new User();
        user.setId(1L);
        user.setName("John");
        user.setLatitude(28.6);
        user.setLongitude(77.2);
        user.setSkills(new HashSet<>(Set.of(java)));

        job1 = new Job();
        job1.setId(10L);
        job1.setTitle("Java Developer");
        job1.setSalary(80000.0);
        job1.setLatitude(28.7);
        job1.setLongitude(77.1);
        job1.setRequiredSkills(new HashSet<>(Set.of(java)));

        job2 = new Job();
        job2.setId(11L);
        job2.setTitle("Python Developer");
        job2.setSalary(90000.0);
        job2.setLatitude(19.0);
        job2.setLongitude(72.8);
        job2.setRequiredSkills(new HashSet<>(Set.of(python)));

        jobDto1 = new JobDto();
        jobDto1.setId(10L);
        jobDto1.setTitle("Java Developer");
        jobDto1.setSalary(80000.0);

        jobDto2 = new JobDto();
        jobDto2.setId(11L);
        jobDto2.setTitle("Python Developer");
        jobDto2.setSalary(90000.0);
    }

    // ── getKNearestJobs ───────────────────────────────────────────────────────
    // Uses findJobsWithinBoundingBox (bounding-box pre-filter), not findAll()

    @Test
    void getKNearestJobs_shouldReturnKNearest_sortedByDistance() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(jobRepository.findJobsWithinBoundingBox(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(List.of(job1, job2));
        when(modelMapper.map(job1, JobDto.class)).thenReturn(jobDto1);
        when(modelMapper.map(job2, JobDto.class)).thenReturn(jobDto2);
        when(haversineUtil.calculateDistance(28.6, 77.2, 28.7, 77.1)).thenReturn(15.0);
        when(haversineUtil.calculateDistance(28.6, 77.2, 19.0, 72.8)).thenReturn(1400.0);
        when(haversineUtil.roundDistance(15.0)).thenReturn(15.0);
        when(haversineUtil.roundDistance(1400.0)).thenReturn(1400.0);

        List<JobDto> result = feedService.getKNearestJobs(1L, 1);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDistanceKm()).isEqualTo(15.0);
    }

    @Test
    void getKNearestJobs_shouldReturnAll_whenKGreaterThanJobCount() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(jobRepository.findJobsWithinBoundingBox(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(List.of(job1, job2));
        when(modelMapper.map(job1, JobDto.class)).thenReturn(jobDto1);
        when(modelMapper.map(job2, JobDto.class)).thenReturn(jobDto2);
        when(haversineUtil.calculateDistance(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(100.0).thenReturn(200.0);
        when(haversineUtil.roundDistance(anyDouble())).thenAnswer(i -> i.getArgument(0));

        List<JobDto> result = feedService.getKNearestJobs(1L, 10);

        assertThat(result).hasSize(2);
    }

    @Test
    void getKNearestJobs_shouldReturnEmpty_whenNoJobs() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(jobRepository.findJobsWithinBoundingBox(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(List.of());

        List<JobDto> result = feedService.getKNearestJobs(1L, 5);

        assertThat(result).isEmpty();
    }

    @Test
    void getKNearestJobs_shouldThrow_whenUserNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> feedService.getKNearestJobs(999L, 5))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    // ── getJobsBySalaryRange ──────────────────────────────────────────────────

    @Test
    void getJobsBySalaryRange_shouldReturnJobsInRange() {
        when(jobRepository.findAllByOrderBySalaryAsc()).thenReturn(List.of(job1, job2));
        when(modelMapper.map(job1, JobDto.class)).thenReturn(jobDto1);
        when(modelMapper.map(job2, JobDto.class)).thenReturn(jobDto2);

        List<JobDto> result = feedService.getJobsBySalaryRange(70000.0, 95000.0, null);

        assertThat(result).hasSize(2);
    }

    @Test
    void getJobsBySalaryRange_shouldReturnOnlyMatchingJobs() {
        when(jobRepository.findAllByOrderBySalaryAsc()).thenReturn(List.of(job1, job2));
        when(modelMapper.map(job1, JobDto.class)).thenReturn(jobDto1);

        List<JobDto> result = feedService.getJobsBySalaryRange(70000.0, 85000.0, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSalary()).isEqualTo(80000.0);
    }

    @Test
    void getJobsBySalaryRange_shouldReturnEmpty_whenNoJobsExist() {
        when(jobRepository.findAllByOrderBySalaryAsc()).thenReturn(List.of());

        List<JobDto> result = feedService.getJobsBySalaryRange(0.0, 100000.0, null);

        assertThat(result).isEmpty();
    }

    @Test
    void getJobsBySalaryRange_shouldReturnEmpty_whenRangeDoesNotMatch() {
        when(jobRepository.findAllByOrderBySalaryAsc()).thenReturn(List.of(job1, job2));

        List<JobDto> result = feedService.getJobsBySalaryRange(200000.0, 300000.0, null);

        assertThat(result).isEmpty();
    }

    // ── getJobsBySkillMatch ───────────────────────────────────────────────────
    // Uses findJobsBySkills (not findAll) when user has skills

    @Test
    void getJobsBySkillMatch_shouldReturnJobsWithMatchingSkills() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        // Only job1 is returned since it matches the user's Java skill
        when(jobRepository.findJobsBySkills(user.getSkills())).thenReturn(List.of(job1));
        when(modelMapper.map(job1, JobDto.class)).thenReturn(jobDto1);

        List<JobDto> result = feedService.getJobsBySkillMatch(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSkillMatchScore()).isEqualTo(100);
    }

    @Test
    void getJobsBySkillMatch_shouldReturn100Score_whenAllSkillsMatch() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(jobRepository.findJobsBySkills(user.getSkills())).thenReturn(List.of(job1));
        when(modelMapper.map(job1, JobDto.class)).thenReturn(jobDto1);

        List<JobDto> result = feedService.getJobsBySkillMatch(1L);

        assertThat(result.get(0).getSkillMatchScore()).isEqualTo(100);
    }

    @Test
    void getJobsBySkillMatch_shouldReturnEmpty_whenUserHasNoSkills() {
        user.setSkills(new HashSet<>());
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        // When skills are empty, impl returns List.of() immediately without querying jobs
        // No call to findJobsBySkills expected

        List<JobDto> result = feedService.getJobsBySkillMatch(1L);

        assertThat(result).isEmpty();
        verify(jobRepository, never()).findJobsBySkills(any());
    }

    @Test
    void getJobsBySkillMatch_shouldThrow_whenUserNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> feedService.getJobsBySkillMatch(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    // ── getRelatedJobs ────────────────────────────────────────────────────────
    // Uses findRelatedJobIdsWithScore (a single JPQL query), not findByJobId/findByUserId

    @Test
    void getRelatedJobs_shouldReturnEmpty_whenNoRelatedJobs() {
        when(jobApplicationRepository.findRelatedJobIdsWithScore(eq(10L), any(Pageable.class)))
                .thenReturn(List.of());

        List<JobDto> result = feedService.getRelatedJobs(10L);

        assertThat(result).isEmpty();
    }

    @Test
    void getRelatedJobs_shouldReturnRelatedJobs_basedOnCommonApplicants() {
        Job job3 = new Job();
        job3.setId(12L);
        job3.setTitle("Related Job");
        job3.setRequiredSkills(new HashSet<>());

        JobDto jobDto3 = new JobDto();
        jobDto3.setId(12L);

        Object[] row = new Object[]{12L, 1L};

        when(jobApplicationRepository.findRelatedJobIdsWithScore(eq(10L), any(Pageable.class)))
                .thenReturn(List.<Object[]>of(row));
        when(jobRepository.findAllById(List.of(12L))).thenReturn(List.of(job3));
        when(modelMapper.map(job3, JobDto.class)).thenReturn(jobDto3);

        List<JobDto> result = feedService.getRelatedJobs(10L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(12L);
    }

    // ── getCombinedFeed ───────────────────────────────────────────────────────
    // Uses findJobsWithinBoundingBoxAndSkills (single DB query), not two separate calls

    @Test
    void getCombinedFeed_shouldThrow_whenUserLocationNotSet() {
        user.setLatitude(null);
        user.setLongitude(null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> feedService.getCombinedFeed(1L, 50.0, 0, 10))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("location");
    }

    @Test
    void getCombinedFeed_shouldReturnJobsWithinDistance() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(jobRepository.findJobsWithinBoundingBoxAndSkillsExcludingCreator(
                anyDouble(), anyDouble(), anyDouble(), anyDouble(), any(), anyBoolean(), any()))
                .thenReturn(List.of(job1, job2));
        when(modelMapper.map(job1, JobDto.class)).thenReturn(jobDto1);
        when(modelMapper.map(job2, JobDto.class)).thenReturn(jobDto2);
        when(haversineUtil.calculateDistance(28.6, 77.2, 28.7, 77.1)).thenReturn(15.0);
        when(haversineUtil.calculateDistance(28.6, 77.2, 19.0, 72.8)).thenReturn(1400.0);
        when(haversineUtil.roundDistance(15.0)).thenReturn(15.0);
        when(haversineUtil.roundDistance(1400.0)).thenReturn(1400.0);

        List<JobDto> result = feedService.getCombinedFeed(1L, 100.0, 0, 10);

        // Only job1 is within 100km
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDistanceKm()).isEqualTo(15.0);
    }

    @Test
    void getCombinedFeed_shouldThrow_whenUserNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> feedService.getCombinedFeed(999L, 50.0, 0, 10))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }
}