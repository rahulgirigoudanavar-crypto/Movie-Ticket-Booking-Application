package com.jsp.book.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jsp.book.service.SeatAvailabilityService;
import com.jsp.book.service.SeatAvailabilityService.SeatAvailabilitySnapshot;
import com.jsp.book.service.SeatAvailabilityService.SeatLayoutStatus;
import com.jsp.book.service.SeatAvailabilityService.SeatStatus;
import com.jsp.book.service.TicketVerificationService;
import com.jsp.book.service.TicketVerificationService.TicketVerificationResult;
import com.jsp.book.service.TicketVerificationService.VerificationStats;
import com.jsp.book.util.RealTimeScanner;
import com.jsp.book.util.RealTimeScanner.QRData;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST API endpoints for real-time scanner, verification, and seat availability
 * Used by mobile apps, web frontend, and admin dashboards
 */
@RestController
@RequestMapping("/api/scanner")
@RequiredArgsConstructor
@Slf4j
public class ScannerController {

    private final RealTimeScanner realTimeScanner;
    private final TicketVerificationService ticketVerificationService;
    private final SeatAvailabilityService seatAvailabilityService;

    // ==================== TICKET VERIFICATION ====================

    /**
     * Verify a single scanned ticket
     * POST /api/scanner/verify
     */
    @PostMapping("/verify")
    public ResponseEntity<TicketVerificationResult> verifyTicket(@RequestBody QRData qrData) {
        log.info("🔍 Received ticket verification request");
        
        if (!realTimeScanner.isValidQRFormat(qrData.getMovieName() + " | " + 
                qrData.getTheaterName() + " | " + qrData.getShowTiming() + " | " + 
                qrData.getSeatNumbers())) {
            return ResponseEntity.badRequest().build();
        }

        TicketVerificationResult result = ticketVerificationService.verifyTicket(qrData);
        return ResponseEntity.ok(result);
    }

    /**
     * Bulk verify multiple tickets
     * POST /api/scanner/verify-bulk
     */
    @PostMapping("/verify-bulk")
    public ResponseEntity<TicketVerificationResult[]> bulkVerify(@RequestBody QRData[] qrDataArray) {
        log.info("📊 Bulk verifying {} tickets", qrDataArray.length);
        
        TicketVerificationResult[] results = ticketVerificationService.bulkVerify(qrDataArray);
        return ResponseEntity.ok(results);
    }

    /**
     * Get verification statistics
     * GET /api/scanner/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<VerificationStats> getStats() {
        log.info("📊 Fetching verification statistics");
        
        VerificationStats stats = ticketVerificationService.getStats();
        return ResponseEntity.ok(stats);
    }

    // ==================== SEAT AVAILABILITY ====================

    /**
     * Get real-time seat availability for a specific show
     * GET /api/scanner/seats/{showId}/availability
     */
    @GetMapping("/seats/{showId}/availability")
    public ResponseEntity<SeatAvailabilitySnapshot> getShowAvailability(@PathVariable Long showId) {
        log.info("📊 Fetching real-time availability for show: {}", showId);
        
        SeatAvailabilitySnapshot snapshot = seatAvailabilityService.getShowAvailability(showId);
        
        if (snapshot == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(snapshot);
    }

    /**
     * Get availability for all shows
     * GET /api/scanner/seats/availability/all
     */
    @GetMapping("/seats/availability/all")
    public ResponseEntity<List<SeatAvailabilitySnapshot>> getAllShowsAvailability() {
        log.info("📊 Fetching real-time availability for all shows");
        
        List<SeatAvailabilitySnapshot> snapshots = seatAvailabilityService.getAllShowsAvailability();
        return ResponseEntity.ok(snapshots);
    }

    /**
     * Get detailed seat layout with booking status
     * GET /api/scanner/seats/{showId}/layout
     */
    @GetMapping("/seats/{showId}/layout")
    public ResponseEntity<SeatLayoutStatus> getSeatLayout(@PathVariable Long showId) {
        log.info("🎫 Fetching real-time seat layout for show: {}", showId);
        
        SeatLayoutStatus layout = seatAvailabilityService.getSeatLayout(showId);
        
        if (layout == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(layout);
    }

    /**
     * Get recent seat booking changes (delta updates)
     * GET /api/scanner/seats/{showId}/changes
     */
    @GetMapping("/seats/{showId}/changes")
    public ResponseEntity<List<SeatStatus>> getRecentChanges(
            @PathVariable Long showId,
            @RequestParam(required = false) Long since) {
        
        log.info("⚡ Fetching recent changes for show: {}", showId);
        
        LocalDateTime lastCheck = since != null ? 
                LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(since), 
                    java.time.ZoneId.systemDefault()) :
                LocalDateTime.now().minusMinutes(1);
        
        List<SeatStatus> changes = seatAvailabilityService.getRecentChanges(showId, lastCheck);
        return ResponseEntity.ok(changes);
    }

    /**
     * Health check for scanner service
     * GET /api/scanner/health
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("✅ Scanner service is running");
    }
}
