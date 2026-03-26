package com.Job.Posting.notification.service;

import com.Job.Posting.dto.notification.NotificationDto;
import com.Job.Posting.entity.User;

import java.util.List;

public interface NotificationService {

    void sendNotification(User user, String title, String body);

    List<NotificationDto> getUserNotifications(Long userId);

    List<NotificationDto> getUnreadNotifications(Long userId);

    Long getUnreadCount(Long userId);

    NotificationDto markAsRead(Long id);

    void markAllAsRead(Long userId);

    void deleteNotification(Long id);
}
