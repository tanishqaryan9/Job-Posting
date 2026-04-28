package com.Job.Posting.otp.repository;

import com.Job.Posting.entity.OtpVerification;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {

    Optional<OtpVerification> findTopByAppUserIdAndTypeAndValueOrderByCreatedAtDesc(
            Long appUserId, String type, String value);

    @Modifying
    @Transactional
    @Query("DELETE FROM OtpVerification o WHERE o.appUser.id = :appUserId AND o.type = :type AND o.value = :value")
    void deleteAllByAppUserIdAndTypeAndValue(Long appUserId, String type, String value);
}