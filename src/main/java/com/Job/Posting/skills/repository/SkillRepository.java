package com.Job.Posting.skills.repository;

import com.Job.Posting.entity.Skills;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SkillRepository extends JpaRepository<Skills,Long> {

    Optional<Skills> findByNameIgnoreCase(@NotBlank(message = "Skills cannot be Blank") @Size(max = 50, message = "Skills name cannot exceed 50 characters") String name);

    boolean existsByNameIgnoreCase(@NotBlank(message = "Skills cannot be Blank") @Size(max = 50, message = "Skills name cannot exceed 50 characters") String name);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query(value = "DELETE FROM user_skills WHERE skill_id = ?1", nativeQuery = true)
    void deleteUserSkillsBySkillId(Long skillId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query(value = "DELETE FROM job_skills WHERE skill_id = ?1", nativeQuery = true)
    void deleteJobSkillsBySkillId(Long skillId);
}
