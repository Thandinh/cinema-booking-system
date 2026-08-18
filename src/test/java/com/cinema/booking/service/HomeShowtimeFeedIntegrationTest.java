package com.cinema.booking.service;

import com.cinema.booking.dto.response.HomeShowtimeFeedResponse;
import com.cinema.booking.entity.Cinema;
import com.cinema.booking.entity.Movie;
import com.cinema.booking.entity.Room;
import com.cinema.booking.entity.Showtime;
import com.cinema.booking.enums.MovieStatus;
import com.cinema.booking.enums.ShowtimeStatus;
import com.cinema.booking.repository.CinemaRepository;
import com.cinema.booking.repository.MovieRepository;
import com.cinema.booking.repository.RoomRepository;
import com.cinema.booking.repository.ShowtimeRepository;
import com.cinema.booking.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "showtime.public-days-ahead=7",
        "showtime.booking-cutoff-minutes=15"
})
@Transactional
class HomeShowtimeFeedIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    ShowtimeService showtimeService;

    @Autowired
    ShowtimeRepository showtimeRepository;

    @Autowired
    MovieRepository movieRepository;

    @Autowired
    CinemaRepository cinemaRepository;

    @Autowired
    RoomRepository roomRepository;

    @Test
    void getHomeShowtimes_shouldSelectNearestBookableDateAndLimitRowsPerMovie() {
        Movie movie = movieRepository.save(Movie.builder()
                .title("Home Feed Movie")
                .duration(120)
                .posterUrl("https://example.com/poster.jpg")
                .ageRating("C13")
                .status(MovieStatus.NOW_SHOWING)
                .isDeleted(false)
                .build());

        Cinema cinema = cinemaRepository.save(Cinema.builder()
                .name("Cinema Quang Nam")
                .address("Hoi An")
                .city("Quảng Nam")
                .isActive(true)
                .isDeleted(false)
                .build());
        Room room = roomRepository.save(Room.builder()
                .cinema(cinema)
                .name("Phòng 01")
                .isDeleted(false)
                .build());

        LocalDate expectedDate = LocalDate.now().plusDays(1);
        List<LocalDateTime> startTimes = List.of(
                expectedDate.atTime(10, 0),
                expectedDate.atTime(12, 30),
                expectedDate.atTime(15, 0));
        showtimeRepository.saveAllAndFlush(startTimes.stream()
                .map(startTime -> Showtime.builder()
                        .movie(movie)
                        .room(room)
                        .startTime(startTime)
                        .endTime(startTime.plusMinutes(120))
                        .basePrice(new BigDecimal("90000.00"))
                        .status(ShowtimeStatus.UPCOMING)
                        .isDeleted(false)
                        .build())
                .toList());

        HomeShowtimeFeedResponse response = showtimeService.getHomeShowtimes(
                "Quảng Nam",
                cinema.getId(),
                null,
                6,
                2);

        assertThat(response.getCity()).isEqualTo("Quảng Nam");
        assertThat(response.getSelectedDate()).isEqualTo(expectedDate);
        assertThat(response.getAvailableDates()).contains(expectedDate);
        assertThat(response.getMovies()).hasSize(1);
        assertThat(response.getMovies().getFirst().getMovieTitle()).isEqualTo("Home Feed Movie");
        assertThat(response.getMovies().getFirst().getShowtimes())
                .hasSize(2)
                .extracting(item -> item.getStartTime().toLocalTime())
                .containsExactly(startTimes.get(0).toLocalTime(), startTimes.get(1).toLocalTime());
    }

    @Test
    void getShowtimesByCinemaId_shouldReturnOnlyUpcomingShowtimesInsidePublicWindow() {
        Movie movie = movieRepository.save(Movie.builder()
                .title("Public Window Movie")
                .duration(120)
                .status(MovieStatus.NOW_SHOWING)
                .isDeleted(false)
                .build());
        Cinema cinema = cinemaRepository.save(Cinema.builder()
                .name("Public Window Cinema")
                .address("Da Nang")
                .city("Da Nang")
                .isActive(true)
                .isDeleted(false)
                .build());
        Room room = roomRepository.save(Room.builder()
                .cinema(cinema)
                .name("Screen 01")
                .isDeleted(false)
                .build());

        LocalDateTime now = LocalDateTime.now();
        Showtime bookable = saveShowtime(movie, room, now.plusHours(2), ShowtimeStatus.UPCOMING);
        saveShowtime(movie, room, now.plusMinutes(5), ShowtimeStatus.UPCOMING);
        saveShowtime(movie, room, now.minusMinutes(10), ShowtimeStatus.ONGOING);
        saveShowtime(movie, room, now.plusHours(3), ShowtimeStatus.CANCELLED);
        saveShowtime(movie, room, now.plusDays(8), ShowtimeStatus.UPCOMING);

        assertThat(showtimeService.getShowtimesByCinemaId(cinema.getId(), PageRequest.of(0, 20)).getContent())
                .extracting(item -> item.getId())
                .containsExactly(bookable.getId());
    }

    private Showtime saveShowtime(
            Movie movie,
            Room room,
            LocalDateTime startTime,
            ShowtimeStatus status) {
        return showtimeRepository.save(Showtime.builder()
                .movie(movie)
                .room(room)
                .startTime(startTime)
                .endTime(startTime.plusMinutes(120))
                .basePrice(new BigDecimal("90000.00"))
                .status(status)
                .isDeleted(false)
                .build());
    }
}
