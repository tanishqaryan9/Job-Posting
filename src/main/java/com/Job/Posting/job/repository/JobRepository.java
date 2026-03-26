package com.Job.Posting.job.repository;

import com.Job.Posting.entity.Job;
import com.Job.Posting.entity.Skills;
import lombok.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface JobRepository extends JpaRepository<Job, Long> {

    @NonNull
    @EntityGraph(attributePaths = {"createdBy", "requiredSkills"})
    Page<Job> findAll(@NonNull Pageable pageable);

    @NonNull
    @EntityGraph(attributePaths = {"createdBy", "requiredSkills"})
    Optional<Job> findById(@NonNull Long id);

    @NonNull
    @EntityGraph(attributePaths = {"createdBy", "requiredSkills"})
    List<Job> findAll();

    //Binary Search
    List<Job> findAllByOrderBySalaryAsc();

    //skill matching(used in feed)
    @Query("select distinct j from Job j JOIN j.requiredSkills s where s in :skills")
    List<Job> findJobsBySkills(@Param("skills") Set<Skills> skills);
}