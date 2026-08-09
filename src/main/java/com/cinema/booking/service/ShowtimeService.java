package com.cinema.booking.service;

import com.cinema.booking.dto.request.ShowtimeCreationRequest;
import com.cinema.booking.dto.request.ShowtimeCancelRequest;
import com.cinema.booking.dto.request.ShowtimeUpdateRequest;
import com.cinema.booking.dto.request.ShowtimeSearchRequest;
import com.cinema.booking.dto.response.HomeShowtimeFeedResponse;
import com.cinema.booking.dto.response.ShowtimeResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ShowtimeService {

    ShowtimeResponse createShowtime(ShowtimeCreationRequest request);

    ShowtimeResponse updateShowtime(UUID id, ShowtimeUpdateRequest request);

    void deleteShowtime(UUID id);

    ShowtimeResponse cancelShowtimeWithPolicy(UUID id, ShowtimeCancelRequest request);

    Page<ShowtimeResponse> getAllShowtimes(ShowtimeSearchRequest request, Pageable pageable);

    HomeShowtimeFeedResponse getHomeShowtimes(
            String city,
            UUID cinemaId,
            LocalDate date,
            int movieLimit,
            int showtimeLimit);

    ShowtimeResponse getShowtimeById(UUID id);

    List<ShowtimeResponse> getShowtimesByMovieId(UUID movieId);
    
    Page<ShowtimeResponse> getShowtimesByCinemaId(UUID cinemaId, Pageable pageable);

    List<ShowtimeResponse> getOpenCheckInShowtimes(UUID cinemaId);
}
