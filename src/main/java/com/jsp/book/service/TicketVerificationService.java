package com.jsp.book.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;
import com.jsp.book.entity.BookedTicket;
import com.jsp.book.repository.TicketRepository;
import com.jsp.book.util.RealTimeScanner.QRData;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for real-time ticket verification at entrance
 * Validates scanned tickets against database records
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TicketVerificationService {

    private final TicketRepository ticketRepository;

    /**
     * Verifies if a scanned ticket is valid
     * 
     * @param qrData parsed QR code data
     * @return TicketVerificationResult with validation details
     */
    public TicketVerificationResult verifyTicket(QRData qrData) {
        log.info("🔍 Verifying ticket: {} @ {} ({})", qrData.getMovieName(), qrData.getTheaterName(), qrData.getShowTiming());

        // Split seat numbers from QR (e.g. "A1,A2" -> ["A1", "A2"])
        String[] qrSeats = qrData.getSeatNumbers().split(",");
        if (qrSeats.length == 0) {
            return TicketVerificationResult.builder()
                    .isValid(false)
                    .status("INVALID_FORMAT")
                    .message("❌ No seats found in QR data")
                    .scannedAt(qrData.getScannedAt())
                    .build();
        }

        // Find ticket by movie, theater, timing AND the first seat number
        // This is necessary because findBy...Containing on a collection checks individual elements
        Optional<BookedTicket> ticketOpt = ticketRepository
                .findByMovieNameAndTheaterNameAndShowTimingAndSeatNumberContaining(
                    qrData.getMovieName(),
                    qrData.getTheaterName(),
                    qrData.getShowTiming(),
                    qrSeats[0].trim()
                );

        if (!ticketOpt.isPresent()) {
            log.warn("❌ Ticket not found in database for seat {}", qrSeats[0]);
            return TicketVerificationResult.builder()
                    .isValid(false)
                    .status("NOT_FOUND")
                    .message("❌ Ticket not found. Invalid or expired.")
                    .scannedAt(qrData.getScannedAt())
                    .build();
        }

        BookedTicket ticket = ticketOpt.get();

        // Verify all seats match
        java.util.Set<String> qrSeatSet = java.util.Arrays.stream(qrSeats)
                .map(String::trim)
                .collect(java.util.stream.Collectors.toSet());
        java.util.Set<String> dbSeatSet = java.util.Arrays.stream(ticket.getSeatNumber())
                .collect(java.util.stream.Collectors.toSet());

        if (!qrSeatSet.equals(dbSeatSet)) {
            log.warn("❌ Seat mismatch! QR: {}, DB: {}", qrSeatSet, dbSeatSet);
            return TicketVerificationResult.builder()
                    .isValid(false)
                    .status("SEAT_MISMATCH")
                    .message("❌ Ticket seats do not match database records.")
                    .scannedAt(qrData.getScannedAt())
                    .build();
        }

        // Check if already used
        if (ticket.isUsed()) {
            log.warn("⚠️ Ticket already used");
            return TicketVerificationResult.builder()
                    .isValid(false)
                    .status("ALREADY_USED")
                    .message("⚠️ Ticket already used on: " + ticket.getUsedAt())
                    .scannedAt(qrData.getScannedAt())
                    .ticket(ticket)
                    .build();
        }

        // Mark ticket as used
        ticket.setUsed(true);
        ticket.setUsedAt(LocalDateTime.now());
        ticketRepository.save(ticket);

        log.info("✅ Ticket verified successfully: {}", ticket.getId());
        return TicketVerificationResult.builder()
                .isValid(true)
                .status("VALID")
                .message("✅ Ticket verified! Entry granted.")
                .scannedAt(qrData.getScannedAt())
                .verifiedAt(ticket.getUsedAt())
                .ticket(ticket)
                .build();
    }

    /**
     * Bulk verify multiple scanned tickets
     * 
     * @param qrDataArray array of scanned QR data
     * @return array of verification results
     */
    public TicketVerificationResult[] bulkVerify(QRData[] qrDataArray) {
        log.info("📊 Bulk verifying {} tickets", qrDataArray.length);
        
        return java.util.Arrays.stream(qrDataArray)
                .map(this::verifyTicket)
                .toArray(TicketVerificationResult[]::new);
    }

    /**
     * Get real-time verification statistics
     */
    public VerificationStats getStats() {
        long totalTickets = ticketRepository.count();
        long usedTickets = ticketRepository.countByIsUsedTrue();
        long validTickets = totalTickets - usedTickets;

        return VerificationStats.builder()
                .totalTickets(totalTickets)
                .usedTickets(usedTickets)
                .availableTickets(validTickets)
                .scanTime(LocalDateTime.now())
                .build();
    }

    /**
     * Result class for ticket verification
     */
    @lombok.Data
    @lombok.Builder
    public static class TicketVerificationResult {
        private boolean isValid;
        private String status;
        private String message;
        private LocalDateTime scannedAt;
        private LocalDateTime verifiedAt;
        private BookedTicket ticket;
    }

    /**
     * Statistics class
     */
    @lombok.Data
    @lombok.Builder
    public static class VerificationStats {
        private long totalTickets;
        private long usedTickets;
        private long availableTickets;
        private LocalDateTime scanTime;
    }
}
