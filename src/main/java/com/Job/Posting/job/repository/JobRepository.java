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

    @EntityGraph(attributePaths = {"createdBy", "requiredSkills"})
    @Query("select j from Job j where " + "j.latitude  between :minLat and :maxLat and " + "j.longitude between :minLon and :maxLon")
    List<Job> findJobsWithinBoundingBox(@Param("minLat") double minLat, @Param("maxLat") double maxLat, @Param("minLon") double minLon, @Param("maxLon") double maxLon);

    // Bounding-box + skill filter for combined feed
    @EntityGraph(attributePaths = {"createdBy", "requiredSkills"})
    @Query("select distinct j from Job j left join j.requiredSkills s where " + "j.latitude  between :minLat and :maxLat and " + "j.longitude between :minLon and :maxLon and " + "(s in :skills or :skillsEmpty = true)")
    List<Job> findJobsWithinBoundingBoxAndSkills(@Param("minLat") double minLat, @Param("maxLat") double maxLat, @Param("minLon") double minLon, @Param("maxLon") double maxLon, @Param("skills") Set<Skills> skills, @Param("skillsEmpty") boolean skillsEmpty);
}