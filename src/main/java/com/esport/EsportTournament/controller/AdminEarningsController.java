//package com.esport.EsportTournament.controller;
//
//import com.esport.EsportTournament.dto.EarningsReportDTO;
//import com.esport.EsportTournament.service.EarningsService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.format.annotation.DateTimeFormat;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.MediaType;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.access.prepost.PreAuthorize;
//import org.springframework.security.core.Authentication;
//import org.springframework.web.bind.annotation.*;
//
//import java.io.IOException;
//import java.time.LocalDateTime;
//import java.time.format.DateTimeFormatter;
//
///**
// * ✅ NEW: Admin Earnings Controller
// * - Real-time earnings tracking
// * - Excel report generation
// * - Financial analytics
// * - IST timezone support
// */
//@Slf4j
//@RestController
//@RequestMapping("/api/admin/earnings")
//@PreAuthorize("hasRole('ADMIN')")
//@RequiredArgsConstructor
//public class AdminEarningsController {
//
//    private final EarningsService earningsService;
//
//    /**
//     * Get today's earnings (IST)
//     */
//    @GetMapping("/today")
//    public ResponseEntity<EarningsReportDTO> getTodayEarnings(Authentication authentication) {
//        String adminUID = (String) authentication.getPrincipal();
//        log.info("Admin {} requesting today's earnings", adminUID);
//
//        EarningsReportDTO report = earningsService.getTodayEarnings();
//        return ResponseEntity.ok(report);
//    }
//
//    /**
//     * Get this week's earnings (IST)
//     */
//    @GetMapping("/week")
//    public ResponseEntity<EarningsReportDTO> getWeeklyEarnings(Authentication authentication) {
//        String adminUID = (String) authentication.getPrincipal();
//        log.info("Admin {} requesting weekly earnings", adminUID);
//
//        EarningsReportDTO report = earningsService.getWeeklyEarnings();
//        return ResponseEntity.ok(report);
//    }
//
//    /**
//     * Get this month's earnings (IST)
//     */
//    @GetMapping("/month")
//    public ResponseEntity<EarningsReportDTO> getMonthlyEarnings(Authentication authentication) {
//        String adminUID = (String) authentication.getPrincipal();
//        log.info("Admin {} requesting monthly earnings", adminUID);
//
//        EarningsReportDTO report = earningsService.getMonthlyEarnings();
//        return ResponseEntity.ok(report);
//    }
//
//    /**
//     * Get custom period earnings
//     */
//    @GetMapping("/custom")
//    public ResponseEntity<EarningsReportDTO> getCustomEarnings(
//            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
//            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
//            Authentication authentication) {
//
//        String adminUID = (String) authentication.getPrincipal();
//        log.info("Admin {} requesting custom earnings from {} to {}", adminUID, start, end);
//
//        EarningsReportDTO report = earningsService.getCustomEarnings(start, end);
//        return ResponseEntity.ok(report);
//    }
//
//    /**
//     * Download Excel report for today
//     */
//    @GetMapping("/export/today")
//    public ResponseEntity<byte[]> exportTodayEarnings(Authentication authentication) {
//        String adminUID = (String) authentication.getPrincipal();
//        log.info("Admin {} exporting today's earnings to Excel", adminUID);
//
//        try {
//            LocalDateTime start = LocalDateTime.now().toLocalDate().atStartOfDay();
//            LocalDateTime end = start.plusDays(1);
//
//            byte[] excelData = earningsService.generateExcelReport(start, end);
//
//            String filename = "earnings_today_" +
//                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) +
//                    ".xlsx";
//
//            HttpHeaders headers = new HttpHeaders();
//            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
//            headers.setContentDispositionFormData("attachment", filename);
//            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
//
//            return new ResponseEntity<>(excelData, headers, HttpStatus.OK);
//
//        } catch (IOException e) {
//            log.error("Failed to generate Excel report", e);
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
//        }
//    }
//
//    /**
//     * Download Excel report for custom period
//     */
//    @GetMapping("/export/custom")
//    public ResponseEntity<byte[]> exportCustomEarnings(
//            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
//            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
//            Authentication authentication) {
//
//        String adminUID = (String) authentication.getPrincipal();
//        log.info("Admin {} exporting custom earnings to Excel: {} to {}", adminUID, start, end);
//
//        try {
//            byte[] excelData = earningsService.generateExcelReport(start, end);
//
//            String filename = "earnings_" +
//                    start.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "_to_" +
//                    end.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".xlsx";
//
//            HttpHeaders headers = new HttpHeaders();
//            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
//            headers.setContentDispositionFormData("attachment", filename);
//            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
//
//            return new ResponseEntity<>(excelData, headers, HttpStatus.OK);
//
//        } catch (IOException e) {
//            log.error("Failed to generate Excel report", e);
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
//        }
//    }
//
//    /**
//     * Get real-time earnings dashboard
//     */
//    @GetMapping("/dashboard")
//    public ResponseEntity<EarningsReportDTO> getEarningsDashboard(Authentication authentication) {
//        String adminUID = (String) authentication.getPrincipal();
//        log.info("Admin {} requesting earnings dashboard", adminUID);
//
//        // Get today's earnings by default
//        EarningsReportDTO report = earningsService.getTodayEarnings();
//        return ResponseEntity.ok(report);
//    }
//}