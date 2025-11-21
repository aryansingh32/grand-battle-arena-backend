package com.esport.EsportTournament.repository;

import com.esport.EsportTournament.model.Notifications;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepo extends JpaRepository<Notifications,Integer> {
}
