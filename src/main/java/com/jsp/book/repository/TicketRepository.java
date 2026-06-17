package com.jsp.book.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.jsp.book.entity.BookedTicket;

public interface TicketRepository extends JpaRepository<BookedTicket, Long> {
	boolean existsByShowId(Long showId);

	boolean existsByScreenName(String screenName);

	boolean existsByMovieName(String movieName);

	boolean existsByTheaterName(String theaterName);

	// Real-time scanner methods
	Optional<BookedTicket> findByMovieNameAndTheaterNameAndShowTimingAndSeatNumberContaining(
			String movieName, String theaterName, String showTiming, String seatNumber);

	Optional<BookedTicket> findByMovieNameAndTheaterNameAndShowTiming(
			String movieName, String theaterName, String showTiming);

	long countByIsUsedTrue();

	@Query("SELECT COUNT(t) FROM BookedTicket t WHERE t.isUsed = true")
	long countUsedTickets();
}
