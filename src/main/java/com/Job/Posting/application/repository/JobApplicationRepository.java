package com.Job.Posting.application.repository;

import com.Job.Posting.entity.JobApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {

    List<JobApplication> findByJobId(Long jobId);
    List<JobApplication> findByUserId(Long userId);

    // Idempotency check
    boolean existsByJobIdAndUserId(Long jobId, Long userId);

    // Pagination
    Page<JobApplication> findAll(Pageable pageable);
}
