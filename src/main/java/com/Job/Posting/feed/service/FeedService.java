package com.Job.Posting.feed.service;

import com.Job.Posting.dto.job.JobDto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface FeedService {
    public List<JobDto> getKNearestJobs(Long userId, int k);

    List<JobDto> getJobsBySalaryRange(Double min, Double max);

    List<JobDto> getJobsBySkillMatch(Long userId);

    List<JobDto> getRelatedJobs(Long jobId);

    List<JobDto> getCombinedFeed(Long userId, Double maxDistanceKm, int page, int size);
}
