package com.Job.Posting.application.repository;

import com.Job.Posting.entity.JobApplication;
import lombok.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {

    List<JobApplication> findByJobId(Long jobId);
    List<JobApplication> findByUserId(Long userId);

    boolean existsByJobIdAndUserId(Long jobId, Long userId);

    @EntityGraph(attributePaths = {"job", "job.createdBy", "job.requiredSkills", "user", "user.skills"})
    @NonNull
    Page<JobApplication> findAll(@NonNull Pageable pageable);

    @Query("select ja2.job.id, count(ja2.job.id) as score " + "from JobApplication ja1 " + "join JobApplication ja2 on ja2.user.id = ja1.user.id " + "where ja1.job.id = :jobId and ja2.job.id <> :jobId " + "group by ja2.job.id " + "order by score desc")
    List<Object[]> findRelatedJobIdsWithScore(@Param("jobId") Long jobId, Pageable pageable);

    // Excludes applications linked to soft-deleted jobs or users so the paginated
    // ID list matches what findAllByIds actually returns (avoids PageImpl count mismatch)
    @Query("select ja.id from JobApplication ja " + "where ja.job.deleted_at IS NULL and ja.user.deleted_at IS NULL")
    Page<Long> findAllIds(Pageable pageable);

    @EntityGraph(attributePaths = {"job", "job.createdBy", "job.requiredSkills", "user", "user.skills"})
    @Query("select ja from JobApplication ja where ja.id in :ids")
    List<JobApplication> findAllByIds(@Param("ids") List<Long> ids);
}