package com.Job.Posting.notification.service.impl;

import com.Job.Posting.config.CacheEvictHelper;
import com.Job.Posting.dto.kafka.ApplicationStatusChangedEvent;
import com.Job.Posting.dto.notification.NotificationDto;
import com.Job.Posting.entity.AppUser;
import com.Job.Posting.entity.Notification;
import com.Job.Posting.entity.User;
import com.Job.Posting.entity.type.StatusType;
import com.Job.Posting.exception.AccessDeniedException;
import com.Job.Posting.exception.ResourceNotFoundException;
import com.Job.Posting.notification.repository.NotificationRepository;
import com.Job.Posting.notification.service.NotificationService;
import com.Job.Posting.user.repository.UserRepository;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final ModelMapper modelMapper;
    private final CacheEvictHelper cacheEvictHelper;
    private final UserRepository userRepository;

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
                log.info("Firebase notification sent: {}", response);
            } catch (FirebaseMessagingException e) {
                log.error("Firebase push failed for user {}: {}", user.getId(), e.getMessage());
            }
        } else {
            log.warn("User {} has no FCM token — notification saved to DB only", user.getId());
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional
    public void onStatusChanged(ApplicationStatusChangedEvent event) {
        log.info("onStatusChanged: applicationId={}, status={}", event.getApplicationId(), event.getNewStatus());
        try {
            User user = userRepository.findById(event.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + event.getUserId()));
            sendNotification(user, "Application Update",
                    buildStatusBody(event.getNewStatus(), event.getJobTitle()));
        } catch (Exception e) {
            log.error("Failed to process onStatusChanged for applicationId={}: {}",
                    event.getApplicationId(), e.getMessage());
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
                }).toList();
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
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + notificationId));
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
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + notificationId));
        requireOwnership(notification.getUser().getId());
        notificationRepository.deleteById(notificationId);
    }

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