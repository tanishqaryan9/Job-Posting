package com.Job.Posting.kafka;

import com.Job.Posting.dto.kafka.ApplicationSubmittedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class ApplicationEventListener {

    private final ApplicationEventProducer producer;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(ApplicationSubmittedEvent event) {
        producer.publishApplicationSubmitted(event);
    }
}