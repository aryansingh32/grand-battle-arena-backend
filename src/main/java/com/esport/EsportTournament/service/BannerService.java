package com.esport.EsportTournament.service;

import com.esport.EsportTournament.dto.BannerDTO;
import com.esport.EsportTournament.model.Banner;
import com.esport.EsportTournament.repository.BannerRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BannerService {

    private final BannerRepo bannerRepo;

    /**
     * Get all active banners (for public display)
     */
    public List<BannerDTO> getActiveBanners() {
        LocalDateTime now = LocalDateTime.now();
        List<Banner> banners = bannerRepo.findActiveBanners(now);
        return banners.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get all banners (admin)
     */
    public List<BannerDTO> getAllBanners() {
        List<Banner> banners = bannerRepo.findAllByOrderByOrderAscCreatedAtDesc();
        return banners.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get banner by ID
     */
    public BannerDTO getBannerById(int id) {
        Banner banner = bannerRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Banner not found: " + id));
        return mapToDTO(banner);
    }

    /**
     * Create new banner
     */
    @Transactional
    public BannerDTO createBanner(BannerDTO dto) {
        Banner banner = mapToEntity(dto);
        banner = bannerRepo.save(banner);
        log.info("Created banner: {}", banner.getId());
        return mapToDTO(banner);
    }

    /**
     * Update banner
     */
    @Transactional
    public BannerDTO updateBanner(int id, BannerDTO dto) {
        Banner banner = bannerRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Banner not found: " + id));
        
        banner.setImageUrl(dto.getImageUrl());
        banner.setTitle(dto.getTitle());
        banner.setDescription(dto.getDescription());
        banner.setActionUrl(dto.getActionUrl());
        banner.setType(dto.getType() != null ? Banner.BannerType.valueOf(dto.getType().toUpperCase()) : Banner.BannerType.IMAGE);
        banner.setOrder(dto.getOrder());
        banner.setActive(dto.isActive());
        banner.setStartDate(dto.getStartDate());
        banner.setEndDate(dto.getEndDate());
        
        banner = bannerRepo.save(banner);
        log.info("Updated banner: {}", id);
        return mapToDTO(banner);
    }

    /**
     * Delete banner
     */
    @Transactional
    public void deleteBanner(int id) {
        if (!bannerRepo.existsById(id)) {
            throw new RuntimeException("Banner not found: " + id);
        }
        bannerRepo.deleteById(id);
        log.info("Deleted banner: {}", id);
    }

    /**
     * Toggle banner active status
     */
    @Transactional
    public BannerDTO toggleBannerStatus(int id) {
        Banner banner = bannerRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Banner not found: " + id));
        banner.setActive(!banner.isActive());
        banner = bannerRepo.save(banner);
        log.info("Toggled banner {} status to: {}", id, banner.isActive());
        return mapToDTO(banner);
    }

    private BannerDTO mapToDTO(Banner banner) {
        BannerDTO dto = new BannerDTO();
        dto.setId(banner.getId());
        dto.setImageUrl(banner.getImageUrl());
        dto.setTitle(banner.getTitle());
        dto.setDescription(banner.getDescription());
        dto.setActionUrl(banner.getActionUrl());
        dto.setType(banner.getType() != null ? banner.getType().name() : "IMAGE");
        dto.setOrder(banner.getOrder());
        dto.setActive(banner.isActive());
        dto.setStartDate(banner.getStartDate());
        dto.setEndDate(banner.getEndDate());
        dto.setCreatedAt(banner.getCreatedAt());
        dto.setUpdatedAt(banner.getUpdatedAt());
        return dto;
    }

    private Banner mapToEntity(BannerDTO dto) {
        Banner banner = new Banner();
        if (dto.getId() > 0) {
            banner.setId(dto.getId());
        }
        banner.setImageUrl(dto.getImageUrl());
        banner.setTitle(dto.getTitle());
        banner.setDescription(dto.getDescription());
        banner.setActionUrl(dto.getActionUrl());
        banner.setType(dto.getType() != null ? Banner.BannerType.valueOf(dto.getType().toUpperCase()) : Banner.BannerType.IMAGE);
        banner.setOrder(dto.getOrder());
        banner.setActive(dto.isActive());
        banner.setStartDate(dto.getStartDate());
        banner.setEndDate(dto.getEndDate());
        return banner;
    }
}

