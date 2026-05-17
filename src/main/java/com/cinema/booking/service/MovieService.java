package com.cinema.booking.service;

import com.cinema.booking.dto.request.MovieCreationRequest;
import com.cinema.booking.dto.request.MovieUpdateRequest;
import com.cinema.booking.dto.response.MovieResponse;
import com.cinema.booking.enums.MovieStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface MovieService {

    MovieResponse createMovie(MovieCreationRequest request);

    MovieResponse updateMovie(UUID id, MovieUpdateRequest request);

    void deleteMovie(UUID id);

    MovieResponse getMovieById(UUID id);

    Page<MovieResponse> getAllMovies(Pageable pageable);

    Page<MovieResponse> getMoviesByStatus(MovieStatus status, Pageable pageable);
}
