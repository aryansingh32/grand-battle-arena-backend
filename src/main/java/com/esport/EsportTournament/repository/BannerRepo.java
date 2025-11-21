package com.esport.EsportTournament.repository;

import com.esport.EsportTournament.model.Banner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BannerRepo extends JpaRepository<Banner, Integer> {
    
    @Query("SELECT b FROM Banner b WHERE b.isActive = true " +
           "AND (b.startDate IS NULL OR b.startDate <= :now) " +
           "AND (b.endDate IS NULL OR b.endDate >= :now) " +
           "ORDER BY b.order ASC, b.createdAt DESC")
    List<Banner> findActiveBanners(LocalDateTime now);

    List<Banner> findAllByOrderByOrderAscCreatedAtDesc();
}

