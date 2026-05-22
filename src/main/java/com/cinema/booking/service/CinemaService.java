package com.cinema.booking.service;

import com.cinema.booking.dto.request.CinemaCreationRequest;
import com.cinema.booking.dto.request.CinemaUpdateRequest;
import com.cinema.booking.dto.response.CinemaResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface CinemaService {
    CinemaResponse createCinema(CinemaCreationRequest request);
    CinemaResponse updateCinema(UUID id, CinemaUpdateRequest request);
    void deleteCinema(UUID id);
    CinemaResponse getCinemaById(UUID id);
    Page<CinemaResponse> getAllCinemas(Pageable pageable, boolean onlyActive);
}
