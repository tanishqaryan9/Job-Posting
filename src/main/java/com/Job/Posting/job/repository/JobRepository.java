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

    // All jobs excluding those created by a specific user (for Jobs tab)
    @EntityGraph(attributePaths = {"createdBy", "requiredSkills"})
    @Query("select j from Job j where j.createdBy.id <> :excludeUserId")
    Page<Job> findAllExcludingCreator(@Param("excludeUserId") Long excludeUserId, Pageable pageable);

    // Binary Search — salary sorted, all jobs
    List<Job> findAllByOrderBySalaryAsc();

    // Binary Search — salary sorted, excluding creator's own jobs (for salary tab in feed)
    @Query("select j from Job j where j.createdBy.id <> :excludeUserId order by j.salary asc")
    List<Job> findAllByOrderBySalaryAscExcludingCreator(@Param("excludeUserId") Long excludeUserId);

    // Skill matching (used in feed)
    @Query("select distinct j from Job j JOIN j.requiredSkills s where s in :skills")
    List<Job> findJobsBySkills(@Param("skills") Set<Skills> skills);

    // Skill matching excluding creator's own jobs
    @Query("select distinct j from Job j JOIN j.requiredSkills s where s in :skills and j.createdBy.id <> :excludeUserId")
    List<Job> findJobsBySkillsExcludingCreator(@Param("skills") Set<Skills> skills, @Param("excludeUserId") Long excludeUserId);

    @EntityGraph(attributePaths = {"createdBy", "requiredSkills"})
    @Query("select j from Job j where " +
            "j.latitude  between :minLat and :maxLat and " +
            "j.longitude between :minLon and :maxLon")
    List<Job> findJobsWithinBoundingBox(@Param("minLat") double minLat, @Param("maxLat") double maxLat,
                                        @Param("minLon") double minLon, @Param("maxLon") double maxLon);

    // Bounding-box excluding creator's own jobs (for nearest + combined feed)
    @EntityGraph(attributePaths = {"createdBy", "requiredSkills"})
    @Query("select j from Job j where " +
            "j.latitude  between :minLat and :maxLat and " +
            "j.longitude between :minLon and :maxLon and " +
            "j.createdBy.id <> :excludeUserId")
    List<Job> findJobsWithinBoundingBoxExcludingCreator(@Param("minLat") double minLat, @Param("maxLat") double maxLat,
                                                        @Param("minLon") double minLon, @Param("maxLon") double maxLon,
                                                        @Param("excludeUserId") Long excludeUserId);

    // Jobs created by a specific user (for employer's "My Posted Jobs" on profile)
    @EntityGraph(attributePaths = {"createdBy", "requiredSkills"})
    @Query("select j from Job j where j.createdBy.id = :userId and j.deleted_at is null order by j.id desc")
    List<Job> findByCreatedById(@Param("userId") Long userId);

    // Bounding-box + skill filter for combined feed — excluding creator
    @EntityGraph(attributePaths = {"createdBy", "requiredSkills"})
    @Query("select distinct j from Job j left join j.requiredSkills s where " +
            "j.latitude  between :minLat and :maxLat and " +
            "j.longitude between :minLon and :maxLon and " +
            "(s in :skills or :skillsEmpty = true) and " +
            "j.createdBy.id <> :excludeUserId")
    List<Job> findJobsWithinBoundingBoxAndSkillsExcludingCreator(
            @Param("minLat") double minLat, @Param("maxLat") double maxLat,
            @Param("minLon") double minLon, @Param("maxLon") double maxLon,
            @Param("skills") Set<Skills> skills, @Param("skillsEmpty") boolean skillsEmpty,
            @Param("excludeUserId") Long excludeUserId);
}
