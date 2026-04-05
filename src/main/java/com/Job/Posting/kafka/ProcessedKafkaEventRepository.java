package com.Job.Posting.kafka;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface ProcessedKafkaEventRepository extends JpaRepository<ProcessedKafkaEvent, String> {

    // Purge events older than the given cutoff — called by scheduled cleanup
    @Modifying
    @Query("delete from ProcessedKafkaEvent e where e.processedAt < :cutoff")
    void deleteOlderThan(@Param("cutoff") LocalDateTime cutoff);
}
