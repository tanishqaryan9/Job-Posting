package com.Job.Posting.kafka;

import com.Job.Posting.dto.kafka.ApplicationSubmittedEvent;
import com.Job.Posting.notification.service.NotificationService;
import com.Job.Posting.user.repository.UserRepository;
import com.Job.Posting.exception.ResourceNotFoundException;
import com.Job.Posting.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApplicationEventListener {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onApplicationSubmitted(ApplicationSubmittedEvent event) {
        log.info("onApplicationSubmitted: applicationId={}", event.getApplicationId());
        try {
            User applicant = userRepository.findById(event.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + event.getUserId()));
            User jobCreator = userRepository.findById(event.getJobCreatorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Job creator not found: " + event.getJobCreatorId()));

            notificationService.sendNotification(applicant,
                    "Application Submitted",
                    "Your application for " + event.getJobTitle() + " has been submitted!");

            notificationService.sendNotification(jobCreator,
                    "New Application",
                    event.getUserName() + " has applied for your job " + event.getJobTitle());

        } catch (Exception e) {
            log.error("Failed to process onApplicationSubmitted for applicationId={}: {}",
                    event.getApplicationId(), e.getMessage());
        }
    }
}