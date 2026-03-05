package com.esport.EsportTournament.repository;

import com.esport.EsportTournament.model.Notifications;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NotificationRepo extends JpaRepository<Notifications,Integer> {
    @Query("SELECT n FROM Notifications n WHERE n.targetAudience IN :audiences ORDER BY n.createdAt DESC")
    List<Notifications> findByTargetAudienceInOrderByCreatedAtDesc(
            @Param("audiences") List<Notifications.TargetAudience> audiences);

    long countByTargetAudience(Notifications.TargetAudience targetAudience);
}
