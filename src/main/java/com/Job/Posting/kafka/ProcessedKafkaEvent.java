package com.Job.Posting.kafka;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "processed_kafka_event")
@AllArgsConstructor
@NoArgsConstructor
public class ProcessedKafkaEvent {

    @Id
    @Column(name = "event_key", length = 255)
    private String eventKey;

    @CreationTimestamp
    @Column(name = "processed_at", updatable = false, nullable = false)
    private LocalDateTime processedAt;

    public ProcessedKafkaEvent(String eventKey) {
        this.eventKey = eventKey;
    }

    public String getEventKey() { return eventKey; }
}
