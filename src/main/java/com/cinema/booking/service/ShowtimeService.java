package com.cinema.booking.service;

import com.cinema.booking.dto.request.ShowtimeCreationRequest;
import com.cinema.booking.dto.request.ShowtimeUpdateRequest;
import com.cinema.booking.dto.response.ShowtimeResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface ShowtimeService {

    ShowtimeResponse createShowtime(ShowtimeCreationRequest request);

    ShowtimeResponse updateShowtime(UUID id, ShowtimeUpdateRequest request);

    void deleteShowtime(UUID id);

    Page<ShowtimeResponse> getAllShowtimes(Pageable pageable);

    ShowtimeResponse getShowtimeById(UUID id);

    List<ShowtimeResponse> getShowtimesByMovieId(UUID movieId);
    
    Page<ShowtimeResponse> getShowtimesByCinemaId(UUID cinemaId, Pageable pageable);

    List<ShowtimeResponse> getOpenCheckInShowtimes(UUID cinemaId);
}
