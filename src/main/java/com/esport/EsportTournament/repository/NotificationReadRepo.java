package com.esport.EsportTournament.repository;

import com.esport.EsportTournament.model.NotificationRead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationReadRepo extends JpaRepository<NotificationRead, Integer> {
    boolean existsByNotification_IdAndFirebaseUserUID(int notificationId, String firebaseUserUID);

    @Query("SELECT nr.notification.id FROM NotificationRead nr " +
            "WHERE nr.firebaseUserUID = :firebaseUserUID AND nr.notification.id IN :notificationIds")
    List<Integer> findReadNotificationIds(
            @Param("firebaseUserUID") String firebaseUserUID,
            @Param("notificationIds") List<Integer> notificationIds);
}
