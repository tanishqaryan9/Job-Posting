package com.Job.Posting;

import com.Job.Posting.config.CacheEvictHelper;
import com.Job.Posting.dto.notification.NotificationDto;
import com.Job.Posting.entity.Notification;
import com.Job.Posting.entity.User;
import com.Job.Posting.exception.ResourceNotFoundException;
import com.Job.Posting.notification.repository.NotificationRepository;
import com.Job.Posting.notification.service.impl.NotificationServiceImpl;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private ModelMapper modelMapper;
    @Mock private CacheEvictHelper cacheEvictHelper;

    @InjectMocks private NotificationServiceImpl notificationService;

    private User user;
    private Notification notification;
    private NotificationDto notificationDto;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setName("John Doe");
        user.setFcmToken(null); // no FCM token by default

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

        // should not throw even without Firebase
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

    // ── getUserNotifications ──────────────────────────────────────────────────

    @Test
    void getUserNotifications_shouldReturnAllNotifications() {
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(notification));
        when(modelMapper.map(notification, NotificationDto.class)).thenReturn(notificationDto);

        List<NotificationDto> result = notificationService.getUserNotifications(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Test Title");
        assertThat(result.get(0).getUserId()).isEqualTo(1L);
    }

    @Test
    void getUserNotifications_shouldReturnEmpty_whenNone() {
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of());

        List<NotificationDto> result = notificationService.getUserNotifications(1L);

        assertThat(result).isEmpty();
    }

    // ── getUnreadNotifications ────────────────────────────────────────────────

    @Test
    void getUnreadNotifications_shouldReturnOnlyUnread() {
        when(notificationRepository.findByUserIdAndIsReadFalse(1L))
                .thenReturn(List.of(notification));
        when(modelMapper.map(notification, NotificationDto.class)).thenReturn(notificationDto);

        List<NotificationDto> result = notificationService.getUnreadNotifications(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserId()).isEqualTo(1L);
    }

    // ── getUnreadCount ────────────────────────────────────────────────────────

    @Test
    void getUnreadCount_shouldReturnCount() {
        when(notificationRepository.countByUserIdAndIsReadFalse(1L)).thenReturn(3L);

        Long count = notificationService.getUnreadCount(1L);

        assertThat(count).isEqualTo(3L);
    }

    @Test
    void getUnreadCount_shouldReturnZero_whenAllRead() {
        when(notificationRepository.countByUserIdAndIsReadFalse(1L)).thenReturn(0L);

        Long count = notificationService.getUnreadCount(1L);

        assertThat(count).isEqualTo(0L);
    }

    // ── markAsRead ────────────────────────────────────────────────────────────

    @Test
    void markAsRead_shouldSetReadTrue_andEvictCache() {
        when(notificationRepository.findById(10L)).thenReturn(Optional.of(notification));
        when(modelMapper.map(notification, NotificationDto.class)).thenReturn(notificationDto);

        notificationService.markAsRead(10L);

        assertThat(notification.isRead()).isTrue();
        verify(cacheEvictHelper).evictUnreadCount(1L);
    }

    @Test
    void markAsRead_shouldThrow_whenNotFound() {
        when(notificationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markAsRead(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    // ── markAllAsRead ─────────────────────────────────────────────────────────

    @Test
    void markAllAsRead_shouldMarkAll_andEvictCache() {
        Notification n2 = new Notification();
        n2.setId(11L);
        n2.setUser(user);
        n2.setRead(false);

        when(notificationRepository.findByUserIdAndIsReadFalse(1L))
                .thenReturn(List.of(notification, n2));

        notificationService.markAllAsRead(1L);

        assertThat(notification.isRead()).isTrue();
        assertThat(n2.isRead()).isTrue();
        verify(cacheEvictHelper).evictUnreadCount(1L);
    }

    @Test
    void markAllAsRead_shouldDoNothing_whenNoUnread() {
        when(notificationRepository.findByUserIdAndIsReadFalse(1L)).thenReturn(List.of());

        notificationService.markAllAsRead(1L);

        verify(cacheEvictHelper).evictUnreadCount(1L);
    }

    // ── deleteNotification ────────────────────────────────────────────────────

    @Test
    void deleteNotification_shouldDelete_whenExists() {
        when(notificationRepository.existsById(10L)).thenReturn(true);

        notificationService.deleteNotification(10L);

        verify(notificationRepository).deleteById(10L);
    }

    @Test
    void deleteNotification_shouldThrow_whenNotFound() {
        when(notificationRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> notificationService.deleteNotification(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");

        verify(notificationRepository, never()).deleteById(any());
    }
}
