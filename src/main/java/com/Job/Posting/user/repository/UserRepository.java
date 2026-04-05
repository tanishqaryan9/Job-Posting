package com.Job.Posting.user.repository;

import com.Job.Posting.entity.User;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
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
}