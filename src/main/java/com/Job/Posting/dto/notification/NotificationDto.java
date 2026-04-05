package com.Job.Posting.dto.notification;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NotificationDto {

    private Long id;
    private Long userId;
    private String title;
    private String body;
    private boolean isRead;
    private LocalDateTime createdAt;
}
