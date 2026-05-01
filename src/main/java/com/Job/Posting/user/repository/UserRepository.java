package com.Job.Posting.user.repository;

import com.Job.Posting.entity.User;
import lombok.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    @NonNull
    @EntityGraph(attributePaths = {"skills"})
    Page<User> findAll(@NonNull Pageable pageable);

    @NonNull
    @EntityGraph(attributePaths = {"skills"})
    Optional<User> findById(@NonNull Long id);

    boolean existsByNumber(String number);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query(value = "DELETE FROM user_skills WHERE user_id = :userId", nativeQuery = true)
    void deleteUserSkillsByUserId(@org.springframework.data.repository.query.Param("userId") Long userId);
}