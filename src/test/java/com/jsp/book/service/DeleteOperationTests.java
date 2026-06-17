package com.jsp.book.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.jsp.book.entity.BookedTicket;
import com.jsp.book.entity.Movie;
import com.jsp.book.entity.Screen;
import com.jsp.book.entity.Show;
import com.jsp.book.entity.Theater;
import com.jsp.book.entity.User;
import com.jsp.book.repository.BookingRepository;
import com.jsp.book.repository.MovieRepository;
import com.jsp.book.repository.ScreenRepository;
import com.jsp.book.repository.SeatRepository;
import com.jsp.book.repository.ShowRepository;
import com.jsp.book.repository.ShowSeatRepository;
import com.jsp.book.repository.TheaterRepository;
import com.jsp.book.repository.TicketRepository;
import com.jsp.book.repository.UserRepository;
import com.jsp.book.repository.QrHelper;
import com.jsp.book.util.CloudinaryHelper;
import com.jsp.book.util.EmailHelper;

import jakarta.servlet.http.HttpSession;

@ExtendWith(MockitoExtension.class)
class DeleteOperationTests {

	@Mock
	private UserRepository userRepository;
	@Mock
	private TheaterRepository theaterRepository;
	@Mock
	private ScreenRepository screenRepository;
	@Mock
	private MovieRepository movieRepository;
	@Mock
	private ShowRepository showRepository;
	@Mock
	private SeatRepository seatRepository;
	@Mock
	private ShowSeatRepository showSeatRepository;
	@Mock
	private TicketRepository ticketRepository;
	@Mock
	private BookingRepository bookingRepository;
	@Mock
	private EmailHelper emailHelper;
	@Mock
	private RedisService redisService;
	@Mock
	private CloudinaryHelper cloudinaryHelper;
	@Mock
	private QrHelper qrHelper;

	@InjectMocks
	private UserServiceImpl userService;

	private HttpSession session;
	private RedirectAttributes redirectAttributes;
	private User adminUser;
	private Theater theater;
	private Screen screen;
	private Movie movie;
	private Show show;

	@BeforeEach
	void setUp() {
		session = mock(HttpSession.class);
		redirectAttributes = mock(RedirectAttributes.class);

		// Create test data
		adminUser = new User();
		adminUser.setId(1L);
		adminUser.setRole("ADMIN");
		adminUser.setEmail("admin@test.com");

		theater = new Theater();
		theater.setId(1L);
		theater.setName("Test Theater");
		theater.setAddress("Test Address");
		theater.setScreenCount(1);

		screen = new Screen();
		screen.setId(1L);
		screen.setName("Screen 1");
		screen.setTheater(theater);

		movie = new Movie();
		movie.setId(1L);
		movie.setName("Test Movie");
		movie.setLanguages("English");

		show = new Show();
		show.setId(1L);
		show.setMovie(movie);
		show.setScreen(screen);
		show.setShowDate(LocalDate.now().plusDays(1));
		show.setStartTime(LocalTime.of(10, 0));
		show.setTicketPrice(150.0);
	}

	@Nested
	@DisplayName("Theater Delete Tests")
	class TheaterDeleteTests {

		@Test
		@DisplayName("Should delete theater when authorized and no screens exist")
		void testDeleteTheaterSuccess() {
			// Setup
			theater.setScreenCount(0);
			when(session.getAttribute("user")).thenReturn(adminUser);
			when(theaterRepository.findById(1L)).thenReturn(Optional.of(theater));

			// Execute
			String result = userService.deleteTheater(1L, session, redirectAttributes);

			// Assert
			assertEquals("redirect:/manage-theaters", result);
			verify(theaterRepository).delete(theater);
			verify(redirectAttributes).addFlashAttribute("pass", "Theater Removed Success");
		}

		@Test
		@DisplayName("Should fail when theater has screens")
		void testDeleteTheaterWithScreens() {
			// Setup
			theater.setScreenCount(1);
			when(session.getAttribute("user")).thenReturn(adminUser);
			when(theaterRepository.findById(1L)).thenReturn(Optional.of(theater));

			// Execute
			String result = userService.deleteTheater(1L, session, redirectAttributes);

			// Assert
			assertEquals("redirect:/manage-theaters", result);
			verify(theaterRepository, never()).delete(theater);
			verify(redirectAttributes).addFlashAttribute("fail", "First Remove The Screens to Remove Theater");
		}

		@Test
		@DisplayName("Should fail when unauthorized (non-admin)")
		void testDeleteTheaterUnauthorized() {
			// Setup
			User regularUser = new User();
			regularUser.setRole("USER");
			when(session.getAttribute("user")).thenReturn(regularUser);

			// Execute
			String result = userService.deleteTheater(1L, session, redirectAttributes);

			// Assert
			assertEquals("redirect:/login", result);
			verify(theaterRepository, never()).delete(any());
		}

		@Test
		@DisplayName("Should fail when theater not found")
		void testDeleteTheaterNotFound() {
			// Setup
			when(session.getAttribute("user")).thenReturn(adminUser);
			when(theaterRepository.findById(1L)).thenReturn(Optional.empty());

			// Execute
			String result = userService.deleteTheater(1L, session, redirectAttributes);

			// Assert
			assertEquals("redirect:/login", result);
			verify(theaterRepository, never()).delete(any());
		}
	}

	@Nested
	@DisplayName("Screen Delete Tests")
	class ScreenDeleteTests {

		@Test
		@DisplayName("Should delete screen when authorized and no shows exist")
		void testDeleteScreenSuccess() {
			// Setup
			when(session.getAttribute("user")).thenReturn(adminUser);
			when(screenRepository.findById(1L)).thenReturn(Optional.of(screen));
			when(showRepository.existsByScreen(screen)).thenReturn(false);
			when(seatRepository.findByScreenOrderBySeatRowAscSeatColumnAsc(screen)).thenReturn(new ArrayList<>());

			// Execute
			String result = userService.deleteScreen(1L, session, redirectAttributes);

			// Assert
			assertEquals("redirect:/manage-screens/1", result);
			verify(screenRepository).delete(screen);
			verify(theaterRepository).save(theater);
			verify(redirectAttributes).addFlashAttribute("pass", "Screen Removed Success");
		}

		@Test
		@DisplayName("Should fail when screen has shows")
		void testDeleteScreenWithShows() {
			// Setup
			when(session.getAttribute("user")).thenReturn(adminUser);
			when(screenRepository.findById(1L)).thenReturn(Optional.of(screen));
			when(showRepository.existsByScreen(screen)).thenReturn(true);

			// Execute
			String result = userService.deleteScreen(1L, session, redirectAttributes);

			// Assert
			assertEquals("redirect:/manage-screens/1", result);
			verify(screenRepository, never()).delete(screen);
			verify(redirectAttributes).addFlashAttribute("fail", "There are Shows Running You can not Delete");
		}

		@Test
		@DisplayName("Should fail when unauthorized")
		void testDeleteScreenUnauthorized() {
			// Setup
			User regularUser = new User();
			regularUser.setRole("USER");
			when(session.getAttribute("user")).thenReturn(regularUser);

			// Execute
			String result = userService.deleteScreen(1L, session, redirectAttributes);

			// Assert
			assertEquals("redirect:/login", result);
			verify(screenRepository, never()).delete(any());
		}

		@Test
		@DisplayName("Should fail when screen not found")
		void testDeleteScreenNotFound() {
			// Setup
			when(session.getAttribute("user")).thenReturn(adminUser);
			when(screenRepository.findById(1L)).thenReturn(Optional.empty());

			// Execute
			String result = userService.deleteScreen(1L, session, redirectAttributes);

			// Assert
			assertEquals("redirect:/manage-theaters", result);
			verify(screenRepository, never()).delete(any());
		}
	}

	@Nested
	@DisplayName("Movie Delete Tests")
	class MovieDeleteTests {

		@Test
		@DisplayName("Should delete movie when authorized and no shows exist")
		void testDeleteMovieSuccess() {
			// Setup
			when(session.getAttribute("user")).thenReturn(adminUser);
			when(movieRepository.findById(1L)).thenReturn(Optional.of(movie));
		when(showRepository.findByMovie(movie)).thenReturn(new ArrayList<>());

			// Execute
			String result = userService.deleteMovie(1L, session, redirectAttributes);

			// Assert
			assertEquals("redirect:/manage-movies", result);
			verify(movieRepository).delete(movie);
			verify(redirectAttributes).addFlashAttribute("pass", "Movie Removed Success");
		}

		@Test
		@DisplayName("Should fail when movie has shows")
		void testDeleteMovieWithShows() {
			// Setup
			when(session.getAttribute("user")).thenReturn(adminUser);
			when(movieRepository.findById(1L)).thenReturn(Optional.of(movie));
		when(showRepository.findByMovie(movie)).thenReturn(List.of(show));

		// Execute
		String result = userService.deleteMovie(1L, session, redirectAttributes);

		// Assert
		assertEquals("redirect:/manage-movies", result);
		verify(movieRepository, never()).delete(movie);
		verify(redirectAttributes).addFlashAttribute("fail", "There are Shows Running So can not Delete");
		}

		@Test
		@DisplayName("Should fail when unauthorized")
		void testDeleteMovieUnauthorized() {
			// Setup
			User regularUser = new User();
			regularUser.setRole("USER");
			when(session.getAttribute("user")).thenReturn(regularUser);

			// Execute
			String result = userService.deleteMovie(1L, session, redirectAttributes);

			// Assert
			assertEquals("redirect:/login", result);
			verify(movieRepository, never()).delete(any());
		}

		@Test
		@DisplayName("Should fail when movie not found")
		void testDeleteMovieNotFound() {
			// Setup
			when(session.getAttribute("user")).thenReturn(adminUser);
			when(movieRepository.findById(1L)).thenReturn(Optional.empty());

			// Execute
			String result = userService.deleteMovie(1L, session, redirectAttributes);

			// Assert
			assertEquals("redirect:/manage-movies", result);
			verify(movieRepository, never()).delete(any());
		}
	}

	@Nested
	@DisplayName("Show Delete Tests")
	class ShowDeleteTests {

		@Test
		@DisplayName("Should delete show when authorized")
		void testDeleteShowSuccess() {
			// Setup
			when(session.getAttribute("user")).thenReturn(adminUser);
			when(showRepository.findById(1L)).thenReturn(Optional.of(show));

			// Execute
			String result = userService.deleteShow(1L, session, redirectAttributes);

			// Assert
			assertEquals("redirect:/manage-shows/1", result);
			verify(showRepository).delete(show);
			verify(redirectAttributes).addFlashAttribute("pass", "Show Removed Success");
		}

		@Test
		@DisplayName("Should fail when unauthorized")
		void testDeleteShowUnauthorized() {
			// Setup
			User regularUser = new User();
			regularUser.setRole("USER");
			when(session.getAttribute("user")).thenReturn(regularUser);

			// Execute
			String result = userService.deleteShow(1L, session, redirectAttributes);

			// Assert
			assertEquals("redirect:/login", result);
			verify(showRepository, never()).delete(any());
		}

		@Test
		@DisplayName("Should fail when show not found")
		void testDeleteShowNotFound() {
			// Setup
			when(session.getAttribute("user")).thenReturn(adminUser);
			when(showRepository.findById(1L)).thenReturn(Optional.empty());

			// Execute
			String result = userService.deleteShow(1L, session, redirectAttributes);

			// Assert
			assertEquals("redirect:/manage-movies", result);
			verify(showRepository, never()).delete(any());
		}
	}

	@Nested
	@DisplayName("Delete with BookedTickets Tests - Issue Detection")
	class DeleteWithBookedTicketsTests {

		@Test
		@DisplayName("Should prevent theater deletion when there are booked tickets")
		void testDeleteTheaterWithBookedTickets() {
			// Setup
			theater.setScreenCount(0);

			when(session.getAttribute("user")).thenReturn(adminUser);
			when(theaterRepository.findById(1L)).thenReturn(Optional.of(theater));
			when(ticketRepository.existsByTheaterName(theater.getName())).thenReturn(true);

			// Execute
			String result = userService.deleteTheater(1L, session, redirectAttributes);

			// Assert
			assertEquals("redirect:/manage-theaters", result);
			verify(theaterRepository, never()).delete(theater);
		}

		@Test
		@DisplayName("Should prevent show deletion when there are booked tickets")
		void testDeleteShowWithBookedTickets() {
			// Setup
			when(session.getAttribute("user")).thenReturn(adminUser);
			when(showRepository.findById(1L)).thenReturn(Optional.of(show));
			when(ticketRepository.existsByShowId(1L)).thenReturn(true);

			// Execute
			String result = userService.deleteShow(1L, session, redirectAttributes);

			// Assert
			assertEquals("redirect:/manage-shows/1", result);
			verify(showRepository, never()).delete(show);
		}

		@Test
		@DisplayName("Should prevent screen deletion when there are booked tickets")
		void testDeleteScreenWithBookedTickets() {
			// Setup
			when(session.getAttribute("user")).thenReturn(adminUser);
			when(screenRepository.findById(1L)).thenReturn(Optional.of(screen));
			when(showRepository.existsByScreen(screen)).thenReturn(false);
			when(ticketRepository.existsByScreenName(screen.getName())).thenReturn(true);

			// Execute
			String result = userService.deleteScreen(1L, session, redirectAttributes);

			// Assert
			assertEquals("redirect:/manage-screens/1", result);
			verify(screenRepository, never()).delete(screen);
		}

		@Test
		@DisplayName("Should prevent movie deletion when there are booked tickets")
		void testDeleteMovieWithBookedTickets() {
			// Setup
			when(session.getAttribute("user")).thenReturn(adminUser);
			when(movieRepository.findById(1L)).thenReturn(Optional.of(movie));
			when(showRepository.findByMovie(movie)).thenReturn(new ArrayList<>());
			when(ticketRepository.existsByMovieName(movie.getName())).thenReturn(true);

			// Execute
			String result = userService.deleteMovie(1L, session, redirectAttributes);

			// Assert
			assertEquals("redirect:/manage-movies", result);
			verify(movieRepository, never()).delete(movie);
		}
	}
}
