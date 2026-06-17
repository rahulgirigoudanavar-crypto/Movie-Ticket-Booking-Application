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
import com.jsp.book.entity.Seat;
import com.jsp.book.entity.Show;
import com.jsp.book.entity.ShowSeat;
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

/**
 * Integration tests for delete operations after fixes
 * Tests all delete operations with proper validation
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Delete Operations - Integration Tests (After Fixes)")
class DeleteOperationsIntegrationTests {

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

		adminUser = new User();
		adminUser.setId(1L);
		adminUser.setRole("ADMIN");
		adminUser.setEmail("admin@test.com");

		theater = new Theater();
		theater.setId(1L);
		theater.setName("PVR Cinema");
		theater.setAddress("123 Main St");
		theater.setScreenCount(1);

		screen = new Screen();
		screen.setId(1L);
		screen.setName("Screen 1");
		screen.setTheater(theater);

		movie = new Movie();
		movie.setId(1L);
		movie.setName("Avatar");
		movie.setLanguages("English");

		show = new Show();
		show.setId(1L);
		show.setMovie(movie);
		show.setScreen(screen);
		show.setShowDate(LocalDate.now().plusDays(1));
		show.setStartTime(LocalTime.of(10, 0));
		show.setEndTime(LocalTime.of(13, 0));
		show.setTicketPrice(150.0);
		show.setSeats(new ArrayList<>());
	}

	@Nested
	@DisplayName("Show Deletion Tests - After Fixes")
	class ShowDeletionTests {

		@Test
		@DisplayName("SUCCESS: Delete show without bookings")
		void testDeleteShowSuccess() {
			when(session.getAttribute("user")).thenReturn(adminUser);
			when(showRepository.findById(1L)).thenReturn(Optional.of(show));
			when(ticketRepository.existsByShowId(1L)).thenReturn(false);

			String result = userService.deleteShow(1L, session, redirectAttributes);

			assertEquals("redirect:/manage-shows/1", result);
			// With empty seats, deleteAll should not be called
			verify(showRepository, times(1)).delete(show);
			verify(redirectAttributes).addFlashAttribute("pass", "Show Removed Success");
		}

		@Test
		@DisplayName("FAILURE: Cannot delete show with booked tickets (NEW FIX)")
		void testDeleteShowWithBookedTickets() {
			when(session.getAttribute("user")).thenReturn(adminUser);
			when(showRepository.findById(1L)).thenReturn(Optional.of(show));
			when(ticketRepository.existsByShowId(1L)).thenReturn(true);

			String result = userService.deleteShow(1L, session, redirectAttributes);

			assertEquals("redirect:/manage-shows/1", result);
			verify(showRepository, never()).delete(any());
			verify(redirectAttributes).addFlashAttribute("fail", "Cannot delete show with booked tickets");
		}

		@Test
		@DisplayName("SUCCESS: ShowSeats are explicitly cleaned before deletion (NEW FIX)")
		void testDeleteShowCleansShowSeats() {
			ShowSeat showSeat = new ShowSeat();
			showSeat.setId(1L);
			show.setSeats(List.of(showSeat));

			when(session.getAttribute("user")).thenReturn(adminUser);
			when(showRepository.findById(1L)).thenReturn(Optional.of(show));
			when(ticketRepository.existsByShowId(1L)).thenReturn(false);

			String result = userService.deleteShow(1L, session, redirectAttributes);

			assertEquals("redirect:/manage-shows/1", result);
			verify(showSeatRepository, times(1)).deleteAll(List.of(showSeat));
			verify(showRepository, times(1)).delete(show);
		}
	}

	@Nested
	@DisplayName("Movie Deletion Tests - After Fixes")
	class MovieDeletionTests {

		@Test
		@DisplayName("SUCCESS: Delete movie without shows and bookings")
		void testDeleteMovieSuccess() {
			when(session.getAttribute("user")).thenReturn(adminUser);
			when(movieRepository.findById(1L)).thenReturn(Optional.of(movie));
			when(showRepository.findByMovie(movie)).thenReturn(new ArrayList<>());
			when(ticketRepository.existsByMovieName("Avatar")).thenReturn(false);

			String result = userService.deleteMovie(1L, session, redirectAttributes);

			assertEquals("redirect:/manage-movies", result);
			verify(movieRepository, times(1)).delete(movie);
			verify(redirectAttributes).addFlashAttribute("pass", "Movie Removed Success");
		}

		@Test
		@DisplayName("FAILURE: Cannot delete movie with active shows (IMPROVED FIX)")
		void testDeleteMovieWithActiveShows() {
			Show pastShow = new Show();
			pastShow.setId(2L);
			pastShow.setShowDate(LocalDate.now().minusDays(1));

			when(session.getAttribute("user")).thenReturn(adminUser);
			when(movieRepository.findById(1L)).thenReturn(Optional.of(movie));
			when(showRepository.findByMovie(movie)).thenReturn(List.of(pastShow));

			String result = userService.deleteMovie(1L, session, redirectAttributes);

			assertEquals("redirect:/manage-movies", result);
			verify(movieRepository, never()).delete(any());
			verify(redirectAttributes).addFlashAttribute("fail", "There are Shows Running So can not Delete");
		}

		@Test
		@DisplayName("FAILURE: Cannot delete movie with booked tickets (NEW FIX)")
		void testDeleteMovieWithBookedTickets() {
			when(session.getAttribute("user")).thenReturn(adminUser);
			when(movieRepository.findById(1L)).thenReturn(Optional.of(movie));
			when(showRepository.findByMovie(movie)).thenReturn(new ArrayList<>());
			when(ticketRepository.existsByMovieName("Avatar")).thenReturn(true);

			String result = userService.deleteMovie(1L, session, redirectAttributes);

			assertEquals("redirect:/manage-movies", result);
			verify(movieRepository, never()).delete(any());
			verify(redirectAttributes).addFlashAttribute("fail", "Cannot delete movie with booked tickets");
		}
	}

	@Nested
	@DisplayName("Screen Deletion Tests - After Fixes")
	class ScreenDeletionTests {

		@Test
		@DisplayName("SUCCESS: Delete screen without shows and bookings")
		void testDeleteScreenSuccess() {
			List<Seat> seats = new ArrayList<>();
			when(session.getAttribute("user")).thenReturn(adminUser);
			when(screenRepository.findById(1L)).thenReturn(Optional.of(screen));
			when(showRepository.existsByScreen(screen)).thenReturn(false);
			when(ticketRepository.existsByScreenName("Screen 1")).thenReturn(false);
			when(seatRepository.findByScreenOrderBySeatRowAscSeatColumnAsc(screen)).thenReturn(seats);

			String result = userService.deleteScreen(1L, session, redirectAttributes);

			assertEquals("redirect:/manage-screens/1", result);
			verify(seatRepository, times(1)).deleteAll(seats);
			verify(screenRepository, times(1)).delete(screen);
			verify(redirectAttributes).addFlashAttribute("pass", "Screen Removed Success");
		}

		@Test
		@DisplayName("FAILURE: Cannot delete screen with active shows")
		void testDeleteScreenWithShows() {
			when(session.getAttribute("user")).thenReturn(adminUser);
			when(screenRepository.findById(1L)).thenReturn(Optional.of(screen));
			when(showRepository.existsByScreen(screen)).thenReturn(true);

			String result = userService.deleteScreen(1L, session, redirectAttributes);

			assertEquals("redirect:/manage-screens/1", result);
			verify(screenRepository, never()).delete(any());
			verify(redirectAttributes).addFlashAttribute("fail", "There are Shows Running You can not Delete");
		}

		@Test
		@DisplayName("FAILURE: Cannot delete screen with booked tickets (NEW FIX)")
		void testDeleteScreenWithBookedTickets() {
			when(session.getAttribute("user")).thenReturn(adminUser);
			when(screenRepository.findById(1L)).thenReturn(Optional.of(screen));
			when(showRepository.existsByScreen(screen)).thenReturn(false);
			when(ticketRepository.existsByScreenName("Screen 1")).thenReturn(true);

			String result = userService.deleteScreen(1L, session, redirectAttributes);

			assertEquals("redirect:/manage-screens/1", result);
			verify(screenRepository, never()).delete(any());
			verify(redirectAttributes).addFlashAttribute("fail", "Cannot delete screen with booked tickets");
		}

		@Test
		@DisplayName("SUCCESS: Theater screen count is updated correctly")
		void testDeleteScreenUpdatesTheaterCount() {
			List<Seat> seats = new ArrayList<>();
			theater.setScreenCount(2);

			when(session.getAttribute("user")).thenReturn(adminUser);
			when(screenRepository.findById(1L)).thenReturn(Optional.of(screen));
			when(showRepository.existsByScreen(screen)).thenReturn(false);
			when(ticketRepository.existsByScreenName("Screen 1")).thenReturn(false);
			when(seatRepository.findByScreenOrderBySeatRowAscSeatColumnAsc(screen)).thenReturn(seats);

			String result = userService.deleteScreen(1L, session, redirectAttributes);

			assertEquals("redirect:/manage-screens/1", result);
			assertEquals(1, theater.getScreenCount());
			verify(theaterRepository, times(1)).save(theater);
		}
	}

	@Nested
	@DisplayName("Theater Deletion Tests - After Fixes")
	class TheaterDeletionTests {

		@Test
		@DisplayName("SUCCESS: Delete theater without screens and bookings")
		void testDeleteTheaterSuccess() {
			theater.setScreenCount(0);

			when(session.getAttribute("user")).thenReturn(adminUser);
			when(theaterRepository.findById(1L)).thenReturn(Optional.of(theater));
			when(ticketRepository.existsByTheaterName("PVR Cinema")).thenReturn(false);

			String result = userService.deleteTheater(1L, session, redirectAttributes);

			assertEquals("redirect:/manage-theaters", result);
			verify(theaterRepository, times(1)).delete(theater);
			verify(redirectAttributes).addFlashAttribute("pass", "Theater Removed Success");
		}

		@Test
		@DisplayName("FAILURE: Cannot delete theater with screens")
		void testDeleteTheaterWithScreens() {
			theater.setScreenCount(1);

			when(session.getAttribute("user")).thenReturn(adminUser);
			when(theaterRepository.findById(1L)).thenReturn(Optional.of(theater));

			String result = userService.deleteTheater(1L, session, redirectAttributes);

			assertEquals("redirect:/manage-theaters", result);
			verify(theaterRepository, never()).delete(any());
			verify(redirectAttributes).addFlashAttribute("fail", "First Remove The Screens to Remove Theater");
		}

		@Test
		@DisplayName("FAILURE: Cannot delete theater with booked tickets (NEW FIX)")
		void testDeleteTheaterWithBookedTickets() {
			theater.setScreenCount(0);

			when(session.getAttribute("user")).thenReturn(adminUser);
			when(theaterRepository.findById(1L)).thenReturn(Optional.of(theater));
			when(ticketRepository.existsByTheaterName("PVR Cinema")).thenReturn(true);

			String result = userService.deleteTheater(1L, session, redirectAttributes);

			assertEquals("redirect:/manage-theaters", result);
			verify(theaterRepository, never()).delete(any());
			verify(redirectAttributes).addFlashAttribute("fail", "Cannot delete theater with booked tickets");
		}
	}

	@Nested
	@DisplayName("Authorization Tests")
	class AuthorizationTests {

		@Test
		@DisplayName("FAILURE: Non-admin cannot delete show")
		void testNonAdminCannotDeleteShow() {
			User regularUser = new User();
			regularUser.setRole("USER");

			when(session.getAttribute("user")).thenReturn(regularUser);

			String result = userService.deleteShow(1L, session, redirectAttributes);

			assertEquals("redirect:/login", result);
			verify(showRepository, never()).delete(any());
		}

		@Test
		@DisplayName("FAILURE: Non-admin cannot delete movie")
		void testNonAdminCannotDeleteMovie() {
			User regularUser = new User();
			regularUser.setRole("USER");

			when(session.getAttribute("user")).thenReturn(regularUser);

			String result = userService.deleteMovie(1L, session, redirectAttributes);

			assertEquals("redirect:/login", result);
			verify(movieRepository, never()).delete(any());
		}

		@Test
		@DisplayName("FAILURE: Non-admin cannot delete screen")
		void testNonAdminCannotDeleteScreen() {
			User regularUser = new User();
			regularUser.setRole("USER");

			when(session.getAttribute("user")).thenReturn(regularUser);

			String result = userService.deleteScreen(1L, session, redirectAttributes);

			assertEquals("redirect:/login", result);
			verify(screenRepository, never()).delete(any());
		}

		@Test
		@DisplayName("FAILURE: Non-admin cannot delete theater")
		void testNonAdminCannotDeleteTheater() {
			User regularUser = new User();
			regularUser.setRole("USER");

			when(session.getAttribute("user")).thenReturn(regularUser);

			String result = userService.deleteTheater(1L, session, redirectAttributes);

			assertEquals("redirect:/login", result);
			verify(theaterRepository, never()).delete(any());
		}
	}

	@Nested
	@DisplayName("Entity Not Found Tests")
	class EntityNotFoundTests {

		@Test
		@DisplayName("FAILURE: Show not found")
		void testDeleteShowNotFound() {
			when(session.getAttribute("user")).thenReturn(adminUser);
			when(showRepository.findById(1L)).thenReturn(Optional.empty());

			String result = userService.deleteShow(1L, session, redirectAttributes);

			assertEquals("redirect:/manage-movies", result);
			verify(showRepository, never()).delete(any());
		}

		@Test
		@DisplayName("FAILURE: Movie not found")
		void testDeleteMovieNotFound() {
			when(session.getAttribute("user")).thenReturn(adminUser);
			when(movieRepository.findById(1L)).thenReturn(Optional.empty());

			String result = userService.deleteMovie(1L, session, redirectAttributes);

			assertEquals("redirect:/manage-movies", result);
			verify(movieRepository, never()).delete(any());
		}

		@Test
		@DisplayName("FAILURE: Screen not found")
		void testDeleteScreenNotFound() {
			when(session.getAttribute("user")).thenReturn(adminUser);
			when(screenRepository.findById(1L)).thenReturn(Optional.empty());

			String result = userService.deleteScreen(1L, session, redirectAttributes);

			assertEquals("redirect:/manage-theaters", result);
			verify(screenRepository, never()).delete(any());
		}

		@Test
		@DisplayName("FAILURE: Theater not found")
		void testDeleteTheaterNotFound() {
			when(session.getAttribute("user")).thenReturn(adminUser);
			when(theaterRepository.findById(1L)).thenReturn(Optional.empty());

			String result = userService.deleteTheater(1L, session, redirectAttributes);

			assertEquals("redirect:/login", result);
			verify(theaterRepository, never()).delete(any());
		}
	}
}
