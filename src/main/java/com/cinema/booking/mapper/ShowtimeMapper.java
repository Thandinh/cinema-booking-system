package com.cinema.booking.mapper;

import com.cinema.booking.dto.request.ShowtimeCreationRequest;
import com.cinema.booking.dto.request.ShowtimeUpdateRequest;
import com.cinema.booking.dto.response.ShowtimeResponse;
import com.cinema.booking.entity.Movie;
import com.cinema.booking.entity.Room;
import com.cinema.booking.entity.Showtime;
import com.cinema.booking.enums.ShowtimeStatus;
import org.springframework.stereotype.Component;

@Component
public class ShowtimeMapper {

    public Showtime toShowtime(ShowtimeCreationRequest request, Movie movie, Room room) {
        return Showtime.builder()
                .movie(movie)
                .room(room)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .basePrice(request.getBasePrice())
                .status(ShowtimeStatus.UPCOMING)
                .isDeleted(false)
                .build();
    }

    public ShowtimeResponse toShowtimeResponse(Showtime showtime) {
        return ShowtimeResponse.builder()
                .id(showtime.getId())
                .movieId(showtime.getMovie().getId())
                .movieTitle(showtime.getMovie().getTitle())
                .moviePosterUrl(showtime.getMovie().getPosterUrl())
                .movieDuration(showtime.getMovie().getDuration())
                .roomId(showtime.getRoom().getId())
                .roomName(showtime.getRoom().getName())
                .cinemaId(showtime.getRoom().getCinema().getId())
                .cinemaName(showtime.getRoom().getCinema().getName())
                .cinemaAddress(showtime.getRoom().getCinema().getAddress())
                .cinemaCity(showtime.getRoom().getCinema().getCity())
                .startTime(showtime.getStartTime())
                .endTime(showtime.getEndTime())
                .basePrice(showtime.getBasePrice())
                .status(showtime.getStatus())
                .build();
    }

    public void updateShowtime(Showtime showtime, ShowtimeUpdateRequest request) {
        if (request.getStartTime() != null) showtime.setStartTime(request.getStartTime());
        if (request.getEndTime() != null)   showtime.setEndTime(request.getEndTime());
        if (request.getBasePrice() != null) showtime.setBasePrice(request.getBasePrice());
        if (request.getStatus() != null)    showtime.setStatus(request.getStatus());
    }
}
