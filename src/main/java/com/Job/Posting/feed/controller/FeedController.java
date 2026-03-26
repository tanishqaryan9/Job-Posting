package com.Job.Posting.feed.controller;

import com.Job.Posting.dto.job.JobDto;
import com.Job.Posting.feed.service.FeedService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/feed")
public class FeedController {

    private final FeedService feedService;

    //min heap
    //get /feed/{userId}/nearest?k=10
    //return K nearest jobs sorted by distance
    @GetMapping("/{userId}/nearest")
    public ResponseEntity<List<JobDto>> getKNearestJobs(@PathVariable Long userId, @RequestParam(defaultValue = "10") int k)
    {
        return ResponseEntity.ok(feedService.getKNearestJobs(userId,k));
    }

    //binary search
    //get /feed/salary?min=10000&max=30000
    //returns jobs within salary range using binary search
    @GetMapping("/salary")
    public ResponseEntity<List<JobDto>> getJobsBySalaryRange(@RequestParam Double min, @RequestParam Double max)
    {
        return ResponseEntity.ok(feedService.getJobsBySalaryRange(min,max));
    }

    //set intersection
    //get /feed/{usedId}/skill-match
    //returns jobs ranked by skill match %
    @GetMapping("/{userId}/skill-match")
    public ResponseEntity<List<JobDto>> getJobsBySkillMatch(@PathVariable Long userId)
    {
        return ResponseEntity.ok(feedService.getJobsBySkillMatch(userId));
    }

    //graph
    //get /feed/jobs/{jobId}/related
    //returns jobs that share common applications
    @GetMapping("/jobs/{jobId}/related")
    public ResponseEntity<List<JobDto>> getRelatedJobs(@PathVariable Long jobId)
    {
        return ResponseEntity.ok(feedService.getRelatedJobs(jobId));
    }

    //combined feed(skills + location)
    //get /feed/{userId}?maxDIstanceKm=10&page=0&size=10
    //returns jobs matching skills and within distance(sorted by nearest)
    @GetMapping("/{userId}")
    public ResponseEntity<List<JobDto>> getCombinedFeed(@PathVariable Long userId, @RequestParam(defaultValue = "10")Double maxDistanceKm, @RequestParam(defaultValue = "0")int page, @RequestParam(defaultValue = "10")int size)
    {
        return ResponseEntity.ok(feedService.getCombinedFeed(userId,maxDistanceKm,page,size));
    }
}
