package com.esport.EsportTournament.service;

import com.esport.EsportTournament.dto.NotificationsDTO;
import com.esport.EsportTournament.exception.ResourceNotFoundException;
import com.esport.EsportTournament.model.NotificationRead;
import com.esport.EsportTournament.model.Notifications;
import com.esport.EsportTournament.model.Users;
import com.esport.EsportTournament.repository.NotificationReadRepo;
import com.esport.EsportTournament.repository.NotificationRepo;
import com.esport.EsportTournament.repository.UsersRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepo notificationRepo;
    @Mock
    private NotificationReadRepo notificationReadRepo;
    @Mock
    private UsersRepo usersRepo;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void getNotificationsForUser_setsReadFlagsFromReadReceiptTable() {
        String uid = "user-123";
        Users user = new Users();
        user.setFirebaseUserUID(uid);

        Notifications n1 = new Notifications();
        n1.setId(1);
        n1.setTitle("A");
        n1.setMessage("M1");
        n1.setTargetAudience(Notifications.TargetAudience.ALL);
        n1.setCreatedAt(LocalDateTime.now().minusMinutes(2));

        Notifications n2 = new Notifications();
        n2.setId(2);
        n2.setTitle("B");
        n2.setMessage("M2");
        n2.setTargetAudience(Notifications.TargetAudience.USER);
        n2.setCreatedAt(LocalDateTime.now().minusMinutes(1));

        when(usersRepo.findByFirebaseUserUID(uid)).thenReturn(Optional.of(user));
        when(notificationRepo.findByTargetAudienceInOrderByCreatedAtDesc(anyList())).thenReturn(List.of(n2, n1));
        when(notificationReadRepo.findReadNotificationIds(eq(uid), anyList())).thenReturn(List.of(2));

        List<NotificationsDTO> result = notificationService.getNotificationsForUser(uid);

        assertEquals(2, result.size());
        assertFalse(result.get(1).isRead());
        assertTrue(result.get(0).isRead());
    }

    @Test
    void markNotificationAsRead_persistsReadReceiptWhenMissing() {
        String uid = "user-123";
        int notificationId = 99;

        Users user = new Users();
        user.setFirebaseUserUID(uid);
        Notifications notification = new Notifications();
        notification.setId(notificationId);

        when(usersRepo.findByFirebaseUserUID(uid)).thenReturn(Optional.of(user));
        when(notificationRepo.findById(notificationId)).thenReturn(Optional.of(notification));
        when(notificationReadRepo.existsByNotification_IdAndFirebaseUserUID(notificationId, uid)).thenReturn(false);

        notificationService.markNotificationAsRead(notificationId, uid);

        ArgumentCaptor<NotificationRead> captor = ArgumentCaptor.forClass(NotificationRead.class);
        verify(notificationReadRepo, times(1)).save(captor.capture());
        assertEquals(uid, captor.getValue().getFirebaseUserUID());
        assertEquals(notificationId, captor.getValue().getNotification().getId());
        assertNotNull(captor.getValue().getReadAt());
    }

    @Test
    void markNotificationAsRead_throwsWhenNotificationMissing() {
        String uid = "user-123";
        int notificationId = 404;

        Users user = new Users();
        user.setFirebaseUserUID(uid);
        when(usersRepo.findByFirebaseUserUID(uid)).thenReturn(Optional.of(user));
        when(notificationRepo.findById(notificationId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> notificationService.markNotificationAsRead(notificationId, uid));
    }
}
