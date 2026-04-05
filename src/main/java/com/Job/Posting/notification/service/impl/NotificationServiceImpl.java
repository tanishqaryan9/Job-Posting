package com.Job.Posting.notification.service.impl;

import com.Job.Posting.config.CacheEvictHelper;
import com.Job.Posting.dto.kafka.ApplicationStatusChangedEvent;
import com.Job.Posting.dto.kafka.ApplicationSubmittedEvent;
import com.Job.Posting.dto.notification.NotificationDto;
import com.Job.Posting.entity.AppUser;
import com.Job.Posting.entity.Notification;
import com.Job.Posting.entity.User;
import com.Job.Posting.entity.type.StatusType;
import com.Job.Posting.exception.AccessDeniedException;
import com.Job.Posting.exception.ResourceNotFoundException;
import com.Job.Posting.kafka.ProcessedKafkaEvent;
import com.Job.Posting.kafka.ProcessedKafkaEventRepository;
import com.Job.Posting.notification.repository.NotificationRepository;
import com.Job.Posting.notification.service.NotificationService;
import com.Job.Posting.user.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final ModelMapper modelMapper;
    private final CacheEvictHelper cacheEvictHelper;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final ProcessedKafkaEventRepository processedEventRepository;

    //Public service methods

    @Override
    @Transactional
    public void sendNotification(User user, String title, String body) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setTitle(title);
        notification.setBody(body);
        notification.setRead(false);
        notificationRepository.save(notification);

        cacheEvictHelper.evictUnreadCount(user.getId());

        if (user.getFcmToken() != null && !user.getFcmToken().isEmpty()) {
            try {
                Message message = Message.builder()
                        .setToken(user.getFcmToken())
                        .setNotification(
                                com.google.firebase.messaging.Notification.builder()
                                        .setTitle(title)
                                        .setBody(body)
                                        .build()
                        ).build();
                String response = FirebaseMessaging.getInstance().send(message);
                log.info("Firebase notification sent successfully: {}", response);
            } catch (FirebaseMessagingException e) {
                log.error("Firebase push failed for user {}: {}", user.getId(), e.getMessage());
            }
        } else {
            log.warn("User {} has no FCM token — notification saved to DB only", user.getId());
        }
    }

    @Override
    public List<NotificationDto> getUserNotifications(Long userId) {
        requireOwnership(userId);
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(n -> {
                    NotificationDto dto = modelMapper.map(n, NotificationDto.class);
                    dto.setUserId(n.getUser().getId());
                    return dto;
                }).toList();
    }

    @Override
    public List<NotificationDto> getUnreadNotifications(Long userId) {
        requireOwnership(userId);
        return notificationRepository.findByUserIdAndIsReadFalse(userId).stream()
                .map(n -> {
                    NotificationDto dto = modelMapper.map(n, NotificationDto.class);
                    dto.setUserId(n.getUser().getId());
                    return dto;
                })
                .toList();
    }

    @Override
    @Cacheable(value = "notifications", key = "#userId")
    public long getUnreadCount(Long userId) {
        requireOwnership(userId);
        log.debug("Cache miss — querying DB for unread count, userId={}", userId);
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Override
    @Transactional
    public NotificationDto markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + notificationId));
        requireOwnership(notification.getUser().getId());
        notification.setRead(true);
        cacheEvictHelper.evictUnreadCount(notification.getUser().getId());
        NotificationDto dto = modelMapper.map(notification, NotificationDto.class);
        dto.setUserId(notification.getUser().getId());
        return dto;
    }

    @Override
    @Transactional
    public void markAllAsRead(Long userId) {
        requireOwnership(userId);
        List<Notification> unread = notificationRepository.findByUserIdAndIsReadFalse(userId);
        unread.forEach(n -> n.setRead(true));
        cacheEvictHelper.evictUnreadCount(userId);
    }

    @Override
    @Transactional
    public void deleteNotification(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + notificationId));
        requireOwnership(notification.getUser().getId());
        notificationRepository.deleteById(notificationId);
    }

    //Kafka consumers

    @KafkaListener(topics = "application.submitted", groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void onApplicationSubmitted(String message) {
        try {
            ApplicationSubmittedEvent event = objectMapper.readValue(message, ApplicationSubmittedEvent.class);
            String eventKey = "application.submitted:" + event.getApplicationId();

            if (!markProcessed(eventKey)) {
                log.info("Duplicate application.submitted for applicationId={} — skipping", event.getApplicationId());
                return;
            }

            log.info("Received application.submitted for applicationId={}", event.getApplicationId());

            User applicant = userRepository.findById(event.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            User jobCreator = userRepository.findById(event.getJobCreatorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Job creator not found"));

            sendNotification(applicant, "Application Submitted",
                    "Your application for " + event.getJobTitle() + " has been submitted!");
            sendNotification(jobCreator, "New Application",
                    event.getUserName() + " has applied for your job " + event.getJobTitle());

        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize application.submitted message", e);
        }
    }

    @KafkaListener(topics = "application.status_changed", groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void onStatusChanged(String message) {
        try {
            ApplicationStatusChangedEvent event = objectMapper.readValue(message, ApplicationStatusChangedEvent.class);
            String eventKey = "application.status_changed:" + event.getApplicationId() + ":" + event.getNewStatus();

            if (!markProcessed(eventKey)) {
                log.info("Duplicate application.status_changed for applicationId={} status={} — skipping",
                        event.getApplicationId(), event.getNewStatus());
                return;
            }

            log.info("Received application.status_changed for applicationId={}, status={}",
                    event.getApplicationId(), event.getNewStatus());

            User user = userRepository.findById(event.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            sendNotification(user, "Application Update",
                    buildStatusBody(event.getNewStatus(), event.getJobTitle()));

        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize application.status_changed message", e);
        }
    }

    //Helpers

    private void requireOwnership(Long profileId) {
        AppUser currentUser = getCurrentUser();
        Long currentProfileId = currentUser.getUserProfile() != null
                ? currentUser.getUserProfile().getId() : null;

        if (currentProfileId == null || !currentProfileId.equals(profileId)) {
            throw new AccessDeniedException("You are not authorised to access these notifications");
        }
    }

    private AppUser getCurrentUser() {
        return (AppUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private boolean markProcessed(String eventKey) {
        try {
            processedEventRepository.saveAndFlush(new ProcessedKafkaEvent(eventKey));
            return true;
        } catch (DataIntegrityViolationException e) {
            return false;
        }
    }

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void purgeOldProcessedEvents() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(7);
        processedEventRepository.deleteOlderThan(cutoff);
        log.info("Purged processed Kafka events older than {}", cutoff);
    }

    private String buildStatusBody(StatusType status, String jobTitle) {
        return switch (status) {
            case SHORTLISTED -> "Congratulations! You have been shortlisted for " + jobTitle;
            case HIRED       -> "Great news! You have been hired for " + jobTitle;
            case REJECTED    -> "Unfortunately, your application for " + jobTitle + " was not selected";
            case PENDING     -> "Your application for " + jobTitle + " is under review";
            default          -> "Your application status for " + jobTitle + " has been updated to " + status;
        };
    }
}