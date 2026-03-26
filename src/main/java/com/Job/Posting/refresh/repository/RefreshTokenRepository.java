package com.Job.Posting.refresh.repository;

import com.Job.Posting.entity.AppUser;
import com.Job.Posting.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);
    void deleteByAppUser(AppUser user);
    void deleteAllByAppUser(AppUser appUser);

    void deleteAllByUsedTrue();
}