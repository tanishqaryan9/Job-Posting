package com.Job.Posting.notification.service.impl;

import com.Job.Posting.config.CacheEvictHelper;
import com.Job.Posting.dto.kafka.ApplicationStatusChangedEvent;
import com.Job.Posting.dto.kafka.ApplicationSubmittedEvent;
import com.Job.Posting.dto.notification.NotificationDto;
import com.Job.Posting.entity.Notification;
import com.Job.Posting.entity.User;
import com.Job.Posting.entity.type.StatusType;
import com.Job.Posting.exception.ResourceNotFoundException;
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
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

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

    @Override
    @Transactional
    public void sendNotification(User user, String title, String body)
    {
        //saving to db first
        Notification notification=new Notification();
        notification.setUser(user);
        notification.setTitle(title);
        notification.setBody(body);
        notification.setRead(false);
        notificationRepository.save(notification);

        cacheEvictHelper.evictUnreadCount(user.getId());

        //send firebase push only if user has FCM token
        if(user.getFcmToken()!=null && !user.getFcmToken().isEmpty())
        {
            try
            {
                Message message=Message.builder()
                        .setToken(user.getFcmToken())
                        .setNotification(
                                com.google.firebase.messaging.Notification.builder()
                                        .setTitle(title)
                                        .setBody(body).build()
                        ).build();
                String response= FirebaseMessaging.getInstance().send(message);
                log.info("Firebase notification sent successfully: {}",response);
            }
            catch (FirebaseMessagingException e)
            {
                //if firebase fails the program will run
                log.error("Firebase notification sent successfully: {}",e.getMessage());
            }
        }
        else
        {
            log.warn("User {} has no FCM token - notification saved to DB only",user.getId());
        }
    }

    @Override
    public List<NotificationDto> getUserNotifications(Long userId) {

        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(n->{
                    NotificationDto dto=modelMapper.map(n,NotificationDto.class);
                    dto.setUserId(n.getUser().getId());
                    return dto;
                }).toList();
    }

    @Override
    public List<NotificationDto> getUnreadNotifications(Long userId) {

        return notificationRepository.findByUserIdAndIsReadFalse(userId)
                .stream()
                .map(n -> {
                    NotificationDto dto = modelMapper.map(n, NotificationDto.class);
                    dto.setUserId(n.getUser().getId());
                    return dto;
                })
                .toList();
    }

    @Override
    @Cacheable(value = "notifications", key = "#userId")
    public Long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Override
    @Transactional
    public NotificationDto markAsRead(Long notificationId) {

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + notificationId));
        notification.setRead(true);
        cacheEvictHelper.evictUnreadCount(notification.getUser().getId());
        NotificationDto dto = modelMapper.map(notification, NotificationDto.class);
        dto.setUserId(notification.getUser().getId());
        return dto;
    }

    @Override
    @Transactional
    public void markAllAsRead(Long userId) {
        List<Notification> unread = notificationRepository.findByUserIdAndIsReadFalse(userId);
        unread.forEach(n -> n.setRead(true));
        cacheEvictHelper.evictUnreadCount(userId);
    }

    @Override
    public void deleteNotification(Long notificationId) {
        if (!notificationRepository.existsById(notificationId)) {
            throw new ResourceNotFoundException("Notification not found with id: " + notificationId);
        }
        notificationRepository.deleteById(notificationId);
    }

    @KafkaListener(topics = "application.submitted", groupId = "notification-group")
    public void onApplicationSubmitted(String message) {
        try {
            ApplicationSubmittedEvent event = objectMapper.readValue(message, ApplicationSubmittedEvent.class);
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

    @KafkaListener(topics = "application.status_changed", groupId = "notification-group")
    public void onStatusChanged(String message) {
        try {
            ApplicationStatusChangedEvent event = objectMapper.readValue(message, ApplicationStatusChangedEvent.class);
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
