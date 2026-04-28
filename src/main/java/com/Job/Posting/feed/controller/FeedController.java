package com.Job.Posting.feed.controller;

import com.Job.Posting.dto.job.JobDto;
import com.Job.Posting.feed.service.FeedService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/feed")
@Validated
public class FeedController {

    private final FeedService feedService;

    // min heap
    // GET /feed/{userId}/nearest?k=10
    @GetMapping("/{userId}/nearest")
    public ResponseEntity<List<JobDto>> getKNearestJobs(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int k) {
        return ResponseEntity.ok(feedService.getKNearestJobs(userId, k));
    }

    // binary search
    // GET /feed/salary?min=10000&max=30000&userId=5
    // userId is optional — when provided, creator's own jobs are excluded
    @GetMapping("/salary")
    public ResponseEntity<List<JobDto>> getJobsBySalaryRange(@RequestParam Double min, @RequestParam Double max, @RequestParam(required = false) Long userId) {
        return ResponseEntity.ok(feedService.getJobsBySalaryRange(min, max, userId));
    }

    // set intersection
    // GET /feed/{userId}/skill-match
    @GetMapping("/{userId}/skill-match")
    public ResponseEntity<List<JobDto>> getJobsBySkillMatch(@PathVariable Long userId) {
        return ResponseEntity.ok(feedService.getJobsBySkillMatch(userId));
    }

    // graph
    // GET /feed/jobs/{jobId}/related
    @GetMapping("/jobs/{jobId}/related")
    public ResponseEntity<List<JobDto>> getRelatedJobs(@PathVariable Long jobId) {
        return ResponseEntity.ok(feedService.getRelatedJobs(jobId));
    }

    // combined feed (skills + location)
    // GET /feed/{userId}?maxDistanceKm=10&page=0&size=10
    @GetMapping("/{userId}")
    public ResponseEntity<List<JobDto>> getCombinedFeed(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "10") Double maxDistanceKm,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(feedService.getCombinedFeed(userId, maxDistanceKm, page, size));
    }
}
