package com.Job.Posting.feed.service.impl;

import com.Job.Posting.application.repository.JobApplicationRepository;
import com.Job.Posting.dto.job.JobDto;
import com.Job.Posting.entity.Job;
import com.Job.Posting.entity.JobApplication;
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
import org.springframework.stereotype.Service;


import java.util.*;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional
public class FeedServiceImpl implements FeedService {

    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final HaversineUtil haversineUtil;
    private final ModelMapper modelMapper;

    //priority queue(min-heap)
    @Override
    @Transactional
    public List<JobDto> getKNearestJobs(Long userId, int k) {
        User user=userRepository.findById(userId).orElseThrow(()->new ResourceNotFoundException("User not found with id: "+userId));
        List<Job> allJobs=jobRepository.findAll();

        //Min heap will keep nearest job at top
        PriorityQueue<JobDto> minHeap=new PriorityQueue<>(Comparator.comparingDouble(JobDto::getDistanceKm));

        for(Job job: allJobs)
        {
            double distance=haversineUtil.calculateDistance(
                    user.getLatitude(),user.getLongitude(),
                    job.getLatitude(),job.getLongitude()
            );
            JobDto dto=modelMapper.map(job,JobDto.class);
            dto.setDistanceKm(haversineUtil.roundDistance(distance));
            minHeap.offer(dto);//auto sort by distance

        }
            //get k nearest from heap
            List<JobDto> result=new ArrayList<>();
            for(int i=0;i<k && !minHeap.isEmpty();i++)
            {
                //gives nearest first from the top
                result.add(minHeap.poll());
            }
        return result;
    }

    //binary tree
    @Override
    @Transactional
    public List<JobDto> getJobsBySalaryRange(Double min, Double max) {
        List<Job> sortedJobs = jobRepository.findAllByOrderBySalaryAsc();
        if (sortedJobs.isEmpty()) return List.of();

        int left = binarySearchLeft(sortedJobs, min);
        int right = binarySearchRight(sortedJobs, max);

        if (left > right || right == -1 || left == -1) return List.of(); // fix: handle no match
        return sortedJobs.subList(left, right + 1).stream()
                .map(job -> modelMapper.map(job, JobDto.class))
                .toList();
    }
    // finds first index where salary >= target
    private int binarySearchLeft(List<Job> jobs, Double target)
    {
        int low=0,high=jobs.size()-1,result=-1;
        while(low<=high)
        {
            int mid=low +(high-low)/2;
            if(jobs.get(mid).getSalary()!=null && jobs.get(mid).getSalary() >=target)
            {
                result=mid;
                high=mid-1;
            }
            else
            {
                low=mid+1;
            }
        }
        return result;
    }
    // finds last index where salary <= target
    private int binarySearchRight(List<Job> jobs, Double target) {
        int low = 0, high = jobs.size() - 1, result = -1; // fix: start at -1 not size-1
        while (low <= high) {
            int mid = low + (high - low) / 2;
            if (jobs.get(mid).getSalary() != null && jobs.get(mid).getSalary() <= target) {
                result = mid;
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        return result;
    }

    //set intersection
    @Override
    @Transactional
    @Cacheable(value = "feed", key = "'skill:' + #userId")
    public List<JobDto> getJobsBySkillMatch(Long userId) {
        User user=userRepository.findById(userId).orElseThrow(()->new ResourceNotFoundException("User not found with id: "+userId));
        Set<Skills> userSkills=user.getSkills();
        List<Job> allJobs=jobRepository.findAll();
        return allJobs.stream().map(job -> {
            JobDto dto=modelMapper.map(job, JobDto.class);
            if(job.getRequiredSkills().isEmpty())
            {
                dto.setSkillMatchScore(0);
                return dto;
            }

            //intersectiom(find common skills)
            Set<Skills> jobSkills=new HashSet<>(job.getRequiredSkills());
            jobSkills.retainAll(userSkills);

            //calculation of match %
            int matchScore=(jobSkills.size()*100)/job.getRequiredSkills().size();
            dto.setSkillMatchScore(matchScore);
            return dto;
        })
                .filter(dto->dto.getSkillMatchScore()>0)//atleast one skill matches
                .sorted(Comparator.comparingInt(JobDto::getSkillMatchScore).reversed())//best match first
                .toList();
    }

    //graph using adjacency map
    @Override
    @Transactional
    public List<JobDto> getRelatedJobs(Long jobId) {
        List<JobApplication> applications=jobApplicationRepository.findByJobId(jobId);
        if(applications.isEmpty()) return List.of();

        //adjacency map where job_id->connection score
        Map<Long,Integer> relatedJobScore=new HashMap<>();
        for(JobApplication application: applications)
        {
            //get all other jobs this user applied to
            List<JobApplication> otherApplications=jobApplicationRepository.findByUserId(application.getUser().getId());
            for(JobApplication other: otherApplications)
            {
                if(!other.getJob().getId().equals(jobId))
                {
                    //incrementing score for more common applications so user gets more related content
                    relatedJobScore.merge(other.getJob().getId(),1,Integer::sum);
                }
            }
        }
        //score by score, most related first(return top 5)
        return relatedJobScore.entrySet().stream().sorted(Map.Entry.<Long, Integer>comparingByValue().reversed()).limit(5)
                .map(entry->jobRepository.findById(entry.getKey()).orElseThrow(()->new ResourceNotFoundException("Job not found")))
                .map(job->modelMapper.map(job,JobDto.class)).toList();
    }

    //combined feed(skills+location together)
    //merging skill matched jobs and nearby jobs and sorts by distance
    @Override
    @Transactional
    @Cacheable(value = "feed", key = "'combined:' + #userId + ':' + #maxDistanceKm + ':' + #page")
    public List<JobDto> getCombinedFeed(Long userId, Double maxDistanceKm, int page, int size) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (user.getLatitude() == null || user.getLongitude() == null) {
            throw new ResourceNotFoundException("User location not set. Please update latitude and longitude first.");
        }

        List<Job> skillMatchJobs = jobRepository.findJobsBySkills(user.getSkills());
        List<Job> allJobs = jobRepository.findAll();

        return Stream.concat(skillMatchJobs.stream(), allJobs.stream())
                .distinct()
                .map(job -> {
                    JobDto dto = modelMapper.map(job, JobDto.class);

                    double distance = haversineUtil.calculateDistance(
                            user.getLatitude(), user.getLongitude(),
                            job.getLatitude(), job.getLongitude()
                    );
                    dto.setDistanceKm(haversineUtil.roundDistance(distance));

                    Set<Skills> jobSkills = new HashSet<>(job.getRequiredSkills());
                    jobSkills.retainAll(user.getSkills());
                    int matchScore = job.getRequiredSkills().isEmpty() ? 0 :
                            (jobSkills.size() * 100) / job.getRequiredSkills().size();
                    dto.setSkillMatchScore(matchScore);

                    return dto;
                })
                .filter(dto -> dto.getDistanceKm() != Double.MAX_VALUE)
                .filter(dto -> dto.getDistanceKm() <= maxDistanceKm)
                .sorted(Comparator.comparingDouble(JobDto::getDistanceKm))
                .skip((long) page * size)
                .limit(size)
                .toList();
    }
}
