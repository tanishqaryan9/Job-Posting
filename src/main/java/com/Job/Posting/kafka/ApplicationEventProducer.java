package com.Job.Posting.kafka;

import com.Job.Posting.dto.kafka.ApplicationStatusChangedEvent;
import com.Job.Posting.dto.kafka.ApplicationSubmittedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publishApplicationSubmitted(ApplicationSubmittedEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send("application.submitted", String.valueOf(event.getApplicationId()), json)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish application.submitted for id={}: {}",
                                    event.getApplicationId(), ex.getMessage());
                            // Don't rethrow — application was already saved to DB
                        } else {
                            log.info("Published application.submitted for applicationId={}",
                                    event.getApplicationId());
                        }
                    });
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize ApplicationSubmittedEvent: {}", e.getMessage());
        }
    }

    public void publishStatusChanged(ApplicationStatusChangedEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send("application.status_changed", String.valueOf(event.getApplicationId()), json)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish application.status_changed for id={}: {}",
                                    event.getApplicationId(), ex.getMessage());
                        } else {
                            log.info("Published application.status_changed for applicationId={}, status={}",
                                    event.getApplicationId(), event.getNewStatus());
                        }
                    });
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize ApplicationStatusChangedEvent: {}", e.getMessage());
        }
    }
}