package com.Job.Posting.application.repository;

import com.Job.Posting.entity.JobApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {

    //graph for related jobs
    List<JobApplication> findByJobId(Long jobId);
    List<JobApplication> findByUserId(Long userId);
}