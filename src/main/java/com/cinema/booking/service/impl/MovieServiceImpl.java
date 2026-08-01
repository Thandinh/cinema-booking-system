package com.cinema.booking.service.impl;

import com.cinema.booking.dto.request.MovieCreationRequest;
import com.cinema.booking.dto.request.MovieUpdateRequest;
import com.cinema.booking.dto.response.MovieResponse;
import com.cinema.booking.entity.Movie;
import com.cinema.booking.configuration.CacheConfig;
import com.cinema.booking.enums.ErrorCode;
import com.cinema.booking.enums.MovieSortMode;
import com.cinema.booking.enums.MovieStatus;
import com.cinema.booking.enums.ShowtimeStatus;
import com.cinema.booking.exception.AppException;
import com.cinema.booking.mapper.MovieMapper;
import com.cinema.booking.repository.MovieRepository;
import com.cinema.booking.repository.ShowtimeRepository;
import com.cinema.booking.service.MovieService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class MovieServiceImpl implements MovieService {

    MovieRepository movieRepository;
    ShowtimeRepository showtimeRepository;
    MovieMapper movieMapper;

    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheConfig.MOVIES, allEntries = true)
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
    @CacheEvict(cacheNames = CacheConfig.MOVIES, allEntries = true)
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
    @CacheEvict(cacheNames = CacheConfig.MOVIES, allEntries = true)
    public void deleteMovie(UUID id) {
        Movie movie = findActiveMovieById(id);
        if (showtimeRepository.existsActiveScheduleByMovieId(
                id,
                List.of(ShowtimeStatus.UPCOMING, ShowtimeStatus.ONGOING),
                LocalDateTime.now())) {
            throw new AppException(ErrorCode.MOVIE_HAS_ACTIVE_SHOWTIMES);
        }
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
    @Cacheable(
            cacheNames = CacheConfig.MOVIES,
            key = "'all:' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort"
    )
    public Page<MovieResponse> getAllMovies(Pageable pageable) {
        return movieRepository.findAllByIsDeletedFalse(pageable)
                .map(movieMapper::toMovieResponse);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = CacheConfig.MOVIES,
            key = "'status:' + #status + ':' + #sortMode + ':' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort",
            condition = "#sortMode == null || #sortMode.name() != 'POPULAR'"
    )
    public Page<MovieResponse> getMoviesByStatus(MovieStatus status, MovieSortMode sortMode, Pageable pageable) {
        MovieSortMode resolvedSortMode = sortMode == null ? MovieSortMode.DEFAULT : sortMode;
        Pageable stablePageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());

        Page<Movie> movies = switch (resolvedSortMode) {
            case POPULAR -> movieRepository.findByStatusOrderByPopularity(status.name(), stablePageable);
            case RELEASE_DATE_ASC -> movieRepository.findByStatusOrderByReleaseDateAsc(status.name(), stablePageable);
            case DEFAULT -> movieRepository.findAllByStatusAndIsDeletedFalse(status, pageable);
        };

        return movies
                .map(movieMapper::toMovieResponse);
    }

    private Movie findActiveMovieById(UUID id) {
        return movieRepository.findActiveById(id)
                .orElseThrow(() -> new AppException(ErrorCode.MOVIE_NOT_FOUND));
    }
}
