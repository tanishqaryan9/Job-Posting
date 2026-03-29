package com.Job.Posting.notification.service.impl;

import com.Job.Posting.config.CacheEvictHelper;
import com.Job.Posting.dto.notification.NotificationDto;
import com.Job.Posting.entity.Notification;
import com.Job.Posting.entity.User;
import com.Job.Posting.exception.ResourceNotFoundException;
import com.Job.Posting.notification.repository.NotificationRepository;
import com.Job.Posting.notification.service.NotificationService;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final ModelMapper modelMapper;
    private final CacheEvictHelper cacheEvictHelper;

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
}
