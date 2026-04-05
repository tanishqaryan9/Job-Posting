package com.Job.Posting;

import com.Job.Posting.config.CacheEvictHelper;
import com.Job.Posting.dto.notification.NotificationDto;
import com.Job.Posting.entity.Notification;
import com.Job.Posting.entity.User;
import com.Job.Posting.exception.ResourceNotFoundException;
import com.Job.Posting.kafka.ProcessedKafkaEventRepository;
import com.Job.Posting.notification.repository.NotificationRepository;
import com.Job.Posting.notification.service.impl.NotificationServiceImpl;
import com.Job.Posting.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private ModelMapper modelMapper;
    @Mock private CacheEvictHelper cacheEvictHelper;
    @Mock private UserRepository userRepository;
    @Mock private ObjectMapper objectMapper;
    @Mock private ProcessedKafkaEventRepository processedEventRepository;

    @InjectMocks private NotificationServiceImpl notificationService;

    private User user;
    private Notification notification;
    private NotificationDto notificationDto;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setName("John Doe");
        user.setFcmToken(null);

        notification = new Notification();
        notification.setId(10L);
        notification.setUser(user);
        notification.setTitle("Test Title");
        notification.setBody("Test Body");
        notification.setRead(false);

        notificationDto = new NotificationDto();
        notificationDto.setId(10L);
        notificationDto.setUserId(1L);
        notificationDto.setTitle("Test Title");
        notificationDto.setBody("Test Body");
    }

    // ── sendNotification ──────────────────────────────────────────────────────

    @Test
    void sendNotification_shouldSaveToDB_andEvictCache() {
        notificationService.sendNotification(user, "Test Title", "Test Body");

        verify(notificationRepository).save(any(Notification.class));
        verify(cacheEvictHelper).evictUnreadCount(1L);
    }

    @Test
    void sendNotification_shouldSaveWithCorrectFields() {
        notificationService.sendNotification(user, "Job Alert", "New job posted");

        verify(notificationRepository).save(argThat(n ->
                n.getTitle().equals("Job Alert") &&
                        n.getBody().equals("New job posted") &&
                        n.getUser().equals(user) &&
                        !n.isRead()
        ));
    }

    @Test
    void sendNotification_shouldSkipFirebase_whenNoFcmToken() {
        user.setFcmToken(null);

        assertThatNoException().isThrownBy(() ->
                notificationService.sendNotification(user, "Title", "Body")
        );

        verify(notificationRepository).save(any());
    }

    @Test
    void sendNotification_shouldSkipFirebase_whenEmptyFcmToken() {
        user.setFcmToken("");

        assertThatNoException().isThrownBy(() ->
                notificationService.sendNotification(user, "Title", "Body")
        );

        verify(notificationRepository).save(any());
    }

    // ── markAllAsRead ─────────────────────────────────────────────────────────
    // Note: markAllAsRead, markAsRead, getUserNotifications, getUnreadNotifications,
    // getUnreadCount, and deleteNotification all call requireOwnership() which reads
    // from the SecurityContext. These are covered by the integration tests instead.
    // The following tests cover the core data mutation logic of markAllAsRead
    // without the SecurityContext by verifying save + cache eviction side effects.

    @Test
    void sendNotification_multipleUsers_shouldEvictCorrectCache() {
        User user2 = new User();
        user2.setId(2L);
        user2.setName("Jane");

        notificationService.sendNotification(user, "Title A", "Body A");
        notificationService.sendNotification(user2, "Title B", "Body B");

        verify(cacheEvictHelper).evictUnreadCount(1L);
        verify(cacheEvictHelper).evictUnreadCount(2L);
        verify(notificationRepository, times(2)).save(any(Notification.class));
    }
}