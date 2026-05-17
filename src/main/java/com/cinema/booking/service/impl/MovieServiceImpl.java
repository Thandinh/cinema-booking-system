package com.cinema.booking.service.impl;

import com.cinema.booking.dto.request.MovieCreationRequest;
import com.cinema.booking.dto.request.MovieUpdateRequest;
import com.cinema.booking.dto.response.MovieResponse;
import com.cinema.booking.entity.Movie;
import com.cinema.booking.enums.ErrorCode;
import com.cinema.booking.enums.MovieStatus;
import com.cinema.booking.exception.AppException;
import com.cinema.booking.mapper.MovieMapper;
import com.cinema.booking.repository.MovieRepository;
import com.cinema.booking.service.MovieService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class MovieServiceImpl implements MovieService {

    MovieRepository movieRepository;
    MovieMapper movieMapper;

    @Override
    @Transactional
    public MovieResponse createMovie(MovieCreationRequest request) {
        if (movieRepository.existsByTitleAndIsDeletedFalse(request.getTitle())) {
            throw new AppException(ErrorCode.MOVIE_TITLE_EXISTED);
        }
        
        Movie movie = movieMapper.toMovie(request);
        Movie saved = movieRepository.save(movie);
        log.info("Created new movie: {}", saved.getTitle());
        return movieMapper.toMovieResponse(saved);
    }

    @Override
    @Transactional
    public MovieResponse updateMovie(UUID id, MovieUpdateRequest request) {
        Movie movie = findActiveMovieById(id);

        if (request.getTitle() != null && !request.getTitle().equals(movie.getTitle())) {
            if (movieRepository.existsByTitleAndIsDeletedFalse(request.getTitle())) {
                throw new AppException(ErrorCode.MOVIE_TITLE_EXISTED);
            }
        }

        movieMapper.updateMovie(movie, request);
        Movie saved = movieRepository.save(movie);
        log.info("Updated movie id={}", id);
        return movieMapper.toMovieResponse(saved);
    }

    @Override
    @Transactional
    public void deleteMovie(UUID id) {
        Movie movie = findActiveMovieById(id);
        movie.setIsDeleted(true);
        movieRepository.save(movie);
        log.info("Soft-deleted movie id={}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public MovieResponse getMovieById(UUID id) {
        Movie movie = findActiveMovieById(id);
        return movieMapper.toMovieResponse(movie);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MovieResponse> getAllMovies(Pageable pageable) {
        return movieRepository.findAllByIsDeletedFalse(pageable)
                .map(movieMapper::toMovieResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MovieResponse> getMoviesByStatus(MovieStatus status, Pageable pageable) {
        return movieRepository.findAllByStatusAndIsDeletedFalse(status, pageable)
                .map(movieMapper::toMovieResponse);
    }

    private Movie findActiveMovieById(UUID id) {
        return movieRepository.findActiveById(id)
                .orElseThrow(() -> new AppException(ErrorCode.MOVIE_NOT_FOUND));
    }
}
