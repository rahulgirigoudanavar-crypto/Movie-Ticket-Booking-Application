package com.jsp.book.util;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.jsp.book.service.TicketVerificationService;
import com.jsp.book.util.RealTimeScanner.QRData;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Command-line admin scanner for real-time ticket verification at entrance
 * Enables manual scanning and verification without web interface
 * 
 * Usage: Uncomment @Component and modify to include in Spring Boot startup
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AdminTicketScanner implements CommandLineRunner {

    private final RealTimeScanner realTimeScanner;
    private final TicketVerificationService ticketVerificationService;
    
    // Set this to true via environment variable ENABLE_TICKET_SCANNER to activate
    private static final boolean ENABLED = false;

    @Override
    public void run(String... args) throws Exception {
        if (!ENABLED) {
            log.debug("ℹ️ Ticket scanner disabled (set ENABLE_TICKET_SCANNER=true to enable)");
            return;
        }

        log.info("╔════════════════════════════════════════╗");
        log.info("║  🎫 REAL-TIME TICKET SCANNER           ║");
        log.info("║  Entry Gate Verification System        ║");
        log.info("╚════════════════════════════════════════╝");
        
        startScannerInterface();
    }

    /**
     * Interactive scanner interface for admin
     */
    private void startScannerInterface() {
        java.util.Scanner scanner = new java.util.Scanner(System.in);
        
        System.out.println("\n📋 Scanner Commands:");
        System.out.println("   scan  - Scan a single ticket");
        System.out.println("   bulk  - Scan multiple tickets");
        System.out.println("   stats - Show verification statistics");
        System.out.println("   exit  - Exit scanner\n");

        boolean running = true;

        while (running) {
            System.out.print("🎫 Command: ");
            String command = scanner.nextLine().trim().toLowerCase();

            switch (command) {
                case "scan":
                    performSingleScan(scanner);
                    break;
                case "bulk":
                    performBulkScan(scanner);
                    break;
                case "stats":
                    displayStats();
                    break;
                case "exit":
                    running = false;
                    log.info("👋 Exiting ticket scanner");
                    break;
                default:
                    System.out.println("❌ Unknown command. Try: scan, bulk, stats, or exit");
            }
        }

        scanner.close();
    }

    /**
     * Single ticket verification
     */
    private void performSingleScan(java.util.Scanner scanner) {
        System.out.println("\n📱 Scan ticket QR or enter: MovieName | TheaterName | ShowTime | SeatNumbers");
        System.out.print("  Enter: ");
        
        String qrData = scanner.nextLine().trim();

        if (!realTimeScanner.isValidQRFormat(qrData)) {
            System.out.println("❌ Invalid QR format!");
            return;
        }

        QRData parsedData = realTimeScanner.parseQRCode(qrData);
        var result = ticketVerificationService.verifyTicket(parsedData);

        displayVerificationResult(result);
    }

    /**
     * Bulk ticket verification
     */
    private void performBulkScan(java.util.Scanner scanner) {
        System.out.println("\n📊 Bulk Scan Mode (type 'done' when finished):");
        java.util.List<QRData> qrList = new java.util.ArrayList<>();

        while (true) {
            System.out.print("  Scan QR or 'done': ");
            String input = scanner.nextLine().trim();

            if ("done".equalsIgnoreCase(input)) {
                break;
            }

            if (realTimeScanner.isValidQRFormat(input)) {
                qrList.add(realTimeScanner.parseQRCode(input));
                System.out.println("  ✅ Added to batch");
            } else {
                System.out.println("  ❌ Invalid format, skipped");
            }
        }

        if (qrList.isEmpty()) {
            System.out.println("❌ No valid tickets scanned");
            return;
        }

        System.out.println("\n📊 Processing " + qrList.size() + " tickets...\n");
        var results = ticketVerificationService.bulkVerify(qrList.toArray(new QRData[0]));

        int valid = 0, invalid = 0;
        for (var result : results) {
            displayVerificationResult(result);
            if (result.isValid()) valid++;
            else invalid++;
        }

        System.out.println("\n📈 Summary: " + valid + " valid, " + invalid + " invalid");
    }

    /**
     * Display verification result
     */
    private void displayVerificationResult(TicketVerificationService.TicketVerificationResult result) {
        System.out.println();
        
        if (result.isValid()) {
            System.out.println("✅ " + result.getMessage());
            System.out.println("   Movie: " + result.getTicket().getMovieName());
            System.out.println("   Theater: " + result.getTicket().getTheaterName());
            System.out.println("   Seats: " + String.join(", ", result.getTicket().getSeatNumber()));
            System.out.println("   Verified: " + result.getVerifiedAt());
        } else {
            System.out.println("❌ " + result.getMessage());
            System.out.println("   Status: " + result.getStatus());
        }
        
        System.out.println();
    }

    /**
     * Display verification statistics
     */
    private void displayStats() {
        var stats = ticketVerificationService.getStats();
        
        System.out.println();
        System.out.println("📊 ═══ TICKET STATISTICS ═══");
        System.out.println("   Total Tickets:     " + stats.getTotalTickets());
        System.out.println("   Used/Verified:     " + stats.getUsedTickets());
        System.out.println("   Available:         " + stats.getAvailableTickets());
        System.out.println("   Usage Rate:        " + 
            String.format("%.1f%%", (double) stats.getUsedTickets() / stats.getTotalTickets() * 100));
        System.out.println();
    }
}
