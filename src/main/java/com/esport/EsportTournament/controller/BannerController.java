package com.esport.EsportTournament.controller;

import com.esport.EsportTournament.dto.BannerDTO;
import com.esport.EsportTournament.service.BannerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/banners")
@RequiredArgsConstructor
public class BannerController {

    private final BannerService bannerService;

    /**
     * Get active banners (public endpoint)
     * GET /api/banners
     */
    @GetMapping
    public ResponseEntity<List<BannerDTO>> getActiveBanners() {
        log.debug("Fetching active banners");
        List<BannerDTO> banners = bannerService.getActiveBanners();
        return ResponseEntity.ok(banners);
    }

    /**
     * Get all banners (admin)
     * GET /api/banners/admin
     */
    @PreAuthorize("hasAuthority('PERM_MANAGE_TOURNAMENTS')")
    @GetMapping("/admin")
    public ResponseEntity<List<BannerDTO>> getAllBanners() {
        log.debug("Admin fetching all banners");
        List<BannerDTO> banners = bannerService.getAllBanners();
        return ResponseEntity.ok(banners);
    }

    /**
     * Get banner by ID (admin)
     * GET /api/banners/{id}
     */
    @PreAuthorize("hasAuthority('PERM_MANAGE_TOURNAMENTS')")
    @GetMapping("/{id}")
    public ResponseEntity<BannerDTO> getBannerById(@PathVariable int id) {
        log.debug("Admin fetching banner: {}", id);
        BannerDTO banner = bannerService.getBannerById(id);
        return ResponseEntity.ok(banner);
    }

    /**
     * Create new banner (admin)
     * POST /api/banners
     */
    @PreAuthorize("hasAuthority('PERM_MANAGE_TOURNAMENTS')")
    @PostMapping
    public ResponseEntity<BannerDTO> createBanner(@Valid @RequestBody BannerDTO dto) {
        log.info("Admin creating new banner");
        BannerDTO created = bannerService.createBanner(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Update banner (admin)
     * PUT /api/banners/{id}
     */
    @PreAuthorize("hasAuthority('PERM_MANAGE_TOURNAMENTS')")
    @PutMapping("/{id}")
    public ResponseEntity<BannerDTO> updateBanner(
            @PathVariable int id,
            @Valid @RequestBody BannerDTO dto) {
        log.info("Admin updating banner: {}", id);
        BannerDTO updated = bannerService.updateBanner(id, dto);
        return ResponseEntity.ok(updated);
    }

    /**
     * Delete banner (admin)
     * DELETE /api/banners/{id}
     */
    @PreAuthorize("hasAuthority('PERM_MANAGE_TOURNAMENTS')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteBanner(@PathVariable int id) {
        log.info("Admin deleting banner: {}", id);
        bannerService.deleteBanner(id);
        return ResponseEntity.ok(Map.of("message", "Banner deleted successfully"));
    }

    /**
     * Toggle banner status (admin)
     * PATCH /api/banners/{id}/toggle
     */
    @PreAuthorize("hasAuthority('PERM_MANAGE_TOURNAMENTS')")
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<BannerDTO> toggleBannerStatus(@PathVariable int id) {
        log.info("Admin toggling banner status: {}", id);
        BannerDTO updated = bannerService.toggleBannerStatus(id);
        return ResponseEntity.ok(updated);
    }
}

