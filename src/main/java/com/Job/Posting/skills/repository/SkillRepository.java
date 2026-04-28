package com.Job.Posting.skills.repository;

import com.Job.Posting.entity.Skills;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SkillRepository extends JpaRepository<Skills,Long> {

    Optional<Skills> findByNameIgnoreCase(@NotBlank(message = "Skills cannot be Blank") @Size(max = 50, message = "Skills name cannot exceed 50 characters") String name);

    boolean existsByNameIgnoreCase(@NotBlank(message = "Skills cannot be Blank") @Size(max = 50, message = "Skills name cannot exceed 50 characters") String name);
}
