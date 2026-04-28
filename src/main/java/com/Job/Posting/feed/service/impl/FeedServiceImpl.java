package com.Job.Posting.feed.service.impl;

import com.Job.Posting.application.repository.JobApplicationRepository;
import com.Job.Posting.dto.job.JobDto;
import com.Job.Posting.entity.Job;
import com.Job.Posting.entity.Skills;
import com.Job.Posting.entity.User;
import com.Job.Posting.exception.ResourceNotFoundException;
import com.Job.Posting.feed.service.FeedService;
import com.Job.Posting.job.repository.JobRepository;
import com.Job.Posting.user.repository.UserRepository;
import com.Job.Posting.util.HaversineUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class FeedServiceImpl implements FeedService {

    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final HaversineUtil haversineUtil;
    private final ModelMapper modelMapper;

    private static final double KM_PER_DEGREE_LAT = 111.0;

    private double[] boundingBox(double lat, double lon, double radiusKm) {
        double deltaLat = radiusKm / KM_PER_DEGREE_LAT;
        double deltaLon = radiusKm / (KM_PER_DEGREE_LAT * Math.cos(Math.toRadians(lat)));
        return new double[]{lat - deltaLat, lat + deltaLat, lon - deltaLon, lon + deltaLon};
    }

    @Override
    @Transactional
    public List<JobDto> getKNearestJobs(Long userId, int k) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (user.getLatitude() == null || user.getLongitude() == null) {
            throw new ResourceNotFoundException("User location not set. Please update latitude and longitude first.");
        }

        double radiusKm = Math.max(k * 50.0, 200.0);
        double[] bb = boundingBox(user.getLatitude(), user.getLongitude(), radiusKm);

        // Exclude jobs created by this user
        List<Job> candidates = jobRepository.findJobsWithinBoundingBoxExcludingCreator(
                bb[0], bb[1], bb[2], bb[3], userId);

        PriorityQueue<JobDto> minHeap = new PriorityQueue<>(Comparator.comparingDouble(JobDto::getDistanceKm));

        for (Job job : candidates) {
            if (job.getLatitude() == null || job.getLongitude() == null) continue;
            double distance = haversineUtil.calculateDistance(
                    user.getLatitude(), user.getLongitude(),
                    job.getLatitude(), job.getLongitude()
            );
            JobDto dto = modelMapper.map(job, JobDto.class);
            dto.setDistanceKm(haversineUtil.roundDistance(distance));
            minHeap.offer(dto);
        }

        List<JobDto> result = new ArrayList<>();
        for (int i = 0; i < k && !minHeap.isEmpty(); i++) {
            result.add(minHeap.poll());
        }
        return result;
    }

    @Override
    @Transactional
        // Fetch jobs sorted by salary excluding the requesting user's own jobs
    public List<JobDto> getJobsBySalaryRange(Double min, Double max, Long userId) {
        List<Job> sortedJobs;
        if (userId != null) {
            sortedJobs = jobRepository.findAllByOrderBySalaryAscExcludingCreator(userId);
        } else {
            sortedJobs = jobRepository.findAllByOrderBySalaryAsc();
        }
        if (sortedJobs.isEmpty()) return List.of();

        int left = binarySearchLeft(sortedJobs, min);
        int right = binarySearchRight(sortedJobs, max);

        if (left == -1 || right == -1 || left > right) return List.of();
        return sortedJobs.subList(left, right + 1).stream()
                .map(job -> modelMapper.map(job, JobDto.class))
                .toList();
    }

    private int binarySearchLeft(List<Job> jobs, Double target) {
        int low = 0, high = jobs.size() - 1, result = -1;
        while (low <= high) {
            int mid = low + (high - low) / 2;
            if (jobs.get(mid).getSalary() != null && jobs.get(mid).getSalary() >= target) {
                result = mid; high = mid - 1;
            } else { low = mid + 1; }
        }
        return result;
    }

    private int binarySearchRight(List<Job> jobs, Double target) {
        int low = 0, high = jobs.size() - 1, result = -1;
        while (low <= high) {
            int mid = low + (high - low) / 2;
            if (jobs.get(mid).getSalary() != null && jobs.get(mid).getSalary() <= target) {
                result = mid; low = mid + 1;
            } else { high = mid - 1; }
        }
        return result;
    }

    @Override
    @Transactional
    @Cacheable(value = "feed", key = "'skill:' + #userId")
    public List<JobDto> getJobsBySkillMatch(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        Set<Skills> userSkills = user.getSkills();

        // Exclude jobs created by this user
        List<Job> candidates = userSkills.isEmpty()
                ? List.of()
                : jobRepository.findJobsBySkillsExcludingCreator(userSkills, userId);

        return candidates.stream().map(job -> {
                    JobDto dto = modelMapper.map(job, JobDto.class);
                    if (job.getRequiredSkills().isEmpty()) {
                        dto.setSkillMatchScore(0);
                        return dto;
                    }
                    Set<Skills> jobSkills = new HashSet<>(job.getRequiredSkills());
                    jobSkills.retainAll(userSkills);
                    int matchScore = (jobSkills.size() * 100) / job.getRequiredSkills().size();
                    dto.setSkillMatchScore(matchScore);
                    return dto;
                })
                .filter(dto -> dto.getSkillMatchScore() > 0)
                .sorted(Comparator.comparingInt(JobDto::getSkillMatchScore).reversed())
                .toList();
    }

    @Override
    @Transactional
    public List<JobDto> getRelatedJobs(Long jobId) {
        List<Object[]> rows = jobApplicationRepository
                .findRelatedJobIdsWithScore(jobId, PageRequest.of(0, 5));
        if (rows.isEmpty()) return List.of();

        List<Long> ids = rows.stream().map(row -> (Long) row[0]).toList();
        return jobRepository.findAllById(ids).stream()
                .map(job -> modelMapper.map(job, JobDto.class))
                .toList();
    }

    @Override
    @Transactional
    @Cacheable(value = "feed", key = "'combined:' + #userId + ':' + #maxDistanceKm + ':' + #page")
    public List<JobDto> getCombinedFeed(Long userId, Double maxDistanceKm, int page, int size) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (user.getLatitude() == null || user.getLongitude() == null) {
            throw new ResourceNotFoundException("User location not set. Please update latitude and longitude first.");
        }

        double[] bb = boundingBox(user.getLatitude(), user.getLongitude(), maxDistanceKm);
        Set<Skills> userSkills = user.getSkills();

        // Exclude jobs created by this user in combined feed
        List<Job> candidates = jobRepository.findJobsWithinBoundingBoxAndSkillsExcludingCreator(
                bb[0], bb[1], bb[2], bb[3], userSkills, userSkills.isEmpty(), userId);

        return candidates.stream()
                .filter(job -> job.getLatitude() != null && job.getLongitude() != null)
                .map(job -> {
                    JobDto dto = modelMapper.map(job, JobDto.class);
                    double distance = haversineUtil.calculateDistance(
                            user.getLatitude(), user.getLongitude(),
                            job.getLatitude(), job.getLongitude()
                    );
                    dto.setDistanceKm(haversineUtil.roundDistance(distance));
                    Set<Skills> jobSkills = new HashSet<>(job.getRequiredSkills());
                    jobSkills.retainAll(userSkills);
                    int matchScore = job.getRequiredSkills().isEmpty() ? 0 :
                            (jobSkills.size() * 100) / job.getRequiredSkills().size();
                    dto.setSkillMatchScore(matchScore);
                    return dto;
                })
                .filter(dto -> dto.getDistanceKm() <= maxDistanceKm)
                .sorted(Comparator.comparingDouble(JobDto::getDistanceKm))
                .skip((long) page * size)
                .limit(size)
                .toList();
    }
}