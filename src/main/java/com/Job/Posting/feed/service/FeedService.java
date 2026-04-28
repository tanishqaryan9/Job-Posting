package com.Job.Posting.feed.service;

import com.Job.Posting.dto.job.JobDto;
import java.util.List;

public interface FeedService {

    List<JobDto> getKNearestJobs(Long userId, int k);

    List<JobDto> getJobsBySalaryRange(Double min, Double max, Long userId);

    List<JobDto> getJobsBySkillMatch(Long userId);

    List<JobDto> getRelatedJobs(Long jobId);

    List<JobDto> getCombinedFeed(Long userId, Double maxDistanceKm, int page, int size);
}
