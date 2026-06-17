package com.jsp.book.util;

import java.util.Scanner;
import java.time.LocalDateTime;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

/**
 * Real-time Scanner for ticket validation at entrance
 * Reads ticket information from console/barcode scanner in real-time
 */
@Component
@Slf4j
public class RealTimeScanner {

    private static final Pattern QR_PATTERN = Pattern.compile(
        "(.+?)\\s*\\|\\s*(.+?)\\s*\\|\\s*(.+?)\\s*\\|\\s*(.+)"
    );

    private final Scanner inputScanner;

    public RealTimeScanner() {
        this.inputScanner = new Scanner(System.in);
    }

    /**
     * Reads QR code data from scanner/console in real-time
     * Expected format: MovieName | TheaterName | ShowTiming | SeatNumbers
     * 
     * @return QR data as string
     */
    public String scanQRCode() {
        log.info("🔍 Waiting for QR code scan...");
        System.out.print("📱 Scan ticket QR code or enter data: ");
        
        String qrData = null;
        if (inputScanner.hasNextLine()) {
            qrData = inputScanner.nextLine().trim();
            log.info("✅ QR Code scanned: {}", qrData);
        }
        return qrData;
    }

    /**
     * Validates if scanned data matches expected QR format
     * 
     * @param qrData the QR code data
     * @return true if valid format
     */
    public boolean isValidQRFormat(String qrData) {
        if (qrData == null || qrData.isEmpty()) {
            return false;
        }
        return QR_PATTERN.matcher(qrData).matches();
    }

    /**
     * Extracts QR components into structured data
     * 
     * @param qrData the QR code data
     * @return QRData object with parsed components
     */
    public QRData parseQRCode(String qrData) {
        var matcher = QR_PATTERN.matcher(qrData);
        
        if (!matcher.matches()) {
            log.warn("❌ Invalid QR format: {}", qrData);
            return null;
        }

        return QRData.builder()
                .movieName(matcher.group(1).trim())
                .theaterName(matcher.group(2).trim())
                .showTiming(matcher.group(3).trim())
                .seatNumbers(matcher.group(4).trim())
                .scannedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Continuous scanning mode - keeps reading QR codes until exit
     * 
     * @return array of scanned QR data
     */
    public String[] continuousScan() {
        log.info("📊 Starting continuous QR scan mode... (type 'exit' to stop)");
        StringBuilder allScans = new StringBuilder();
        
        while (true) {
            System.out.print("\n📱 Scan QR (or 'exit'): ");
            String input = inputScanner.nextLine().trim();
            
            if ("exit".equalsIgnoreCase(input)) {
                log.info("🛑 Exiting scan mode");
                break;
            }
            
            if (isValidQRFormat(input)) {
                QRData data = parseQRCode(input);
                log.info("✅ Scanned: {} - {} - {} - Seats: {}", 
                    data.getMovieName(), data.getTheaterName(), 
                    data.getShowTiming(), data.getSeatNumbers());
                allScans.append(input).append("\n");
            } else {
                log.warn("❌ Invalid format, try again");
            }
        }
        
        return allScans.toString().split("\n");
    }

    /**
     * Closes the scanner resource
     */
    public void close() {
        if (inputScanner != null) {
            inputScanner.close();
            log.info("✅ Scanner closed");
        }
    }

    /**
     * Inner class to hold parsed QR data
     */
    @lombok.Data
    @lombok.Builder
    public static class QRData {
        private String movieName;
        private String theaterName;
        private String showTiming;
        private String seatNumbers;
        private LocalDateTime scannedAt;
    }
}
