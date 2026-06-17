package com.jsp.book.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import com.jsp.book.entity.Show;
import com.jsp.book.entity.ShowSeat;
import com.jsp.book.repository.ShowRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for real-time seat availability monitoring
 * Provides live seat status updates for shows
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SeatAvailabilityService {

    private final ShowRepository showRepository;

    /**
     * Get real-time seat availability for a specific show
     * 
     * @param showId the show ID
     * @return SeatAvailabilitySnapshot with current seat status
     */
    public SeatAvailabilitySnapshot getShowAvailability(Long showId) {
        log.info("📊 Fetching real-time availability for show: {}", showId);

        Optional<Show> showOpt = showRepository.findById(showId);
        if (!showOpt.isPresent()) {
            log.warn("❌ Show not found: {}", showId);
            return null;
        }

        Show show = showOpt.get();
        List<ShowSeat> seats = show.getSeats();

        int totalSeats = seats.size();
        long bookedSeats = seats.stream().filter(ShowSeat::isBooked).count();
        long availableSeats = totalSeats - bookedSeats;

        // Group by category for detailed breakdown
        Map<String, SeatCategoryStatus> categoryStatus = groupBySeatCategory(seats);

        return SeatAvailabilitySnapshot.builder()
                .showId(showId)
                .movieName(show.getMovie().getName())
                .screenName(show.getScreen().getName())
                .showTiming(show.getStartTime() + " - " + show.getEndTime())
                .totalSeats(totalSeats)
                .bookedSeats((int) bookedSeats)
                .availableSeats((int) availableSeats)
                .occupancyPercentage((double) bookedSeats / totalSeats * 100)
                .categoryStatus(categoryStatus)
                .lastUpdated(LocalDateTime.now())
                .build();
    }

    /**
     * Get availability for all shows
     * 
     * @return List of all show availability snapshots
     */
    public List<SeatAvailabilitySnapshot> getAllShowsAvailability() {
        log.info("📊 Fetching real-time availability for all shows");
        
        List<Show> allShows = showRepository.findAll();
        List<SeatAvailabilitySnapshot> snapshots = new ArrayList<>();

        for (Show show : allShows) {
            SeatAvailabilitySnapshot snapshot = getShowAvailability(show.getId());
            if (snapshot != null) {
                snapshots.add(snapshot);
            }
        }

        log.info("✅ Retrieved availability for {} shows", snapshots.size());
        return snapshots;
    }

    /**
     * Get seat layout with real-time booking status
     * 
     * @param showId the show ID
     * @return SeatLayoutStatus with seat positions and booking status
     */
    public SeatLayoutStatus getSeatLayout(Long showId) {
        log.info("🎫 Fetching real-time seat layout for show: {}", showId);

        Optional<Show> showOpt = showRepository.findById(showId);
        if (!showOpt.isPresent()) {
            return null;
        }

        Show show = showOpt.get();
        Map<String, Map<String, SeatStatus>> layout = new HashMap<>();

        // Organize seats by row and position
        for (ShowSeat showSeat : show.getSeats()) {
            String seatNumber = showSeat.getSeat().getSeatNumber();
            String row = seatNumber.substring(0, 1);
            String position = seatNumber.substring(1);

            layout.computeIfAbsent(row, k -> new HashMap<>())
                    .put(position, SeatStatus.builder()
                            .seatNumber(seatNumber)
                            .isBooked(showSeat.isBooked())
                            .category(showSeat.getSeat().getCategory())
                            .build());
        }

        return SeatLayoutStatus.builder()
                .showId(showId)
                .seatLayout(layout)
                .lastUpdated(LocalDateTime.now())
                .build();
    }

    /**
     * Monitor seat booking in real-time (delta updates)
     * Returns only changes since last check
     * 
     * @param showId the show ID
     * @param lastCheck timestamp of last check
     * @return list of changed seats
     */
    public List<SeatStatus> getRecentChanges(Long showId, LocalDateTime lastCheck) {
        log.info("⚡ Fetching recent seat changes for show: {}", showId);

        Optional<Show> showOpt = showRepository.findById(showId);
        if (!showOpt.isPresent()) {
            return new ArrayList<>();
        }

        Show show = showOpt.get();
        List<SeatStatus> changes = new ArrayList<>();

        for (ShowSeat showSeat : show.getSeats()) {
            // In a production system, you'd check modification timestamps
            // For now, return all current bookings
            changes.add(SeatStatus.builder()
                    .seatNumber(showSeat.getSeat().getSeatNumber())
                    .isBooked(showSeat.isBooked())
                    .category(showSeat.getSeat().getCategory())
                    .build());
        }

        return changes;
    }

    /**
     * Get seat category breakdown
     */
    private Map<String, SeatCategoryStatus> groupBySeatCategory(List<ShowSeat> seats) {
        Map<String, SeatCategoryStatus> categoryMap = new HashMap<>();

        for (ShowSeat seat : seats) {
            String category = seat.getSeat().getCategory();
            categoryMap.computeIfAbsent(category, k -> new SeatCategoryStatus(category, 0, 0))
                    .incrementTotal();

            if (seat.isBooked()) {
                categoryMap.get(category).incrementBooked();
            }
        }

        return categoryMap;
    }

    /**
     * Snapshot of seat availability
     */
    @lombok.Data
    @lombok.Builder
    public static class SeatAvailabilitySnapshot {
        private Long showId;
        private String movieName;
        private String screenName;
        private String showTiming;
        private int totalSeats;
        private int bookedSeats;
        private int availableSeats;
        private double occupancyPercentage;
        private Map<String, SeatCategoryStatus> categoryStatus;
        private LocalDateTime lastUpdated;
    }

    /**
     * Status for a specific seat category
     */
    @lombok.Data
    public static class SeatCategoryStatus {
        private String category;
        private int totalSeats;
        private int bookedSeats;

        public SeatCategoryStatus(String category, int total, int booked) {
            this.category = category;
            this.totalSeats = total;
            this.bookedSeats = booked;
        }

        public void incrementTotal() {
            this.totalSeats++;
        }

        public void incrementBooked() {
            this.bookedSeats++;
        }

        public int getAvailableSeats() {
            return totalSeats - bookedSeats;
        }
    }

    /**
     * Individual seat status
     */
    @lombok.Data
    @lombok.Builder
    public static class SeatStatus {
        private String seatNumber;
        private boolean isBooked;
        private String category;
    }

    /**
     * Complete seat layout
     */
    @lombok.Data
    @lombok.Builder
    public static class SeatLayoutStatus {
        private Long showId;
        private Map<String, Map<String, SeatStatus>> seatLayout;
        private LocalDateTime lastUpdated;
    }
}
