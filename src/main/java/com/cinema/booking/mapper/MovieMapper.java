package com.cinema.booking.mapper;

import com.cinema.booking.dto.request.MovieCreationRequest;
import com.cinema.booking.dto.request.MovieUpdateRequest;
import com.cinema.booking.dto.response.MovieResponse;
import com.cinema.booking.entity.Movie;
import org.springframework.stereotype.Component;

@Component
public class MovieMapper {

    public Movie toMovie(MovieCreationRequest request) {
        if (request == null) return null;
        return Movie.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .duration(request.getDuration())
                .genre(request.getGenre())
                .releaseDate(request.getReleaseDate())
                .posterUrl(request.getPosterUrl())
                .status(request.getStatus())
                .director(request.getDirector())
                .actors(request.getActors())
                .language(request.getLanguage())
                .subtitleLanguage(request.getSubtitleLanguage())
                .country(request.getCountry())
                .ageRating(request.getAgeRating())
                .trailerUrl(request.getTrailerUrl())
                .ratingImdb(request.getRatingImdb())
                .isDeleted(false)
                .build();
    }

    public MovieResponse toMovieResponse(Movie movie) {
        if (movie == null) return null;
        return MovieResponse.builder()
                .id(movie.getId())
                .title(movie.getTitle())
                .description(movie.getDescription())
                .duration(movie.getDuration())
                .genre(movie.getGenre())
                .releaseDate(movie.getReleaseDate())
                .posterUrl(movie.getPosterUrl())
                .status(movie.getStatus())
                .director(movie.getDirector())
                .actors(movie.getActors())
                .language(movie.getLanguage())
                .subtitleLanguage(movie.getSubtitleLanguage())
                .country(movie.getCountry())
                .ageRating(movie.getAgeRating())
                .trailerUrl(movie.getTrailerUrl())
                .ratingImdb(movie.getRatingImdb())
                .createdAt(movie.getCreatedAt())
                .updatedAt(movie.getUpdatedAt())
                .build();
    }

    public void updateMovie(Movie movie, MovieUpdateRequest request) {
        if (request == null) return;
        
        if (request.getTitle() != null) movie.setTitle(request.getTitle());
        if (request.getDescription() != null) movie.setDescription(request.getDescription());
        if (request.getDuration() != null) movie.setDuration(request.getDuration());
        if (request.getGenre() != null) movie.setGenre(request.getGenre());
        if (request.getReleaseDate() != null) movie.setReleaseDate(request.getReleaseDate());
        if (request.getPosterUrl() != null) movie.setPosterUrl(request.getPosterUrl());
        if (request.getStatus() != null) movie.setStatus(request.getStatus());
        if (request.getDirector() != null) movie.setDirector(request.getDirector());
        if (request.getActors() != null) movie.setActors(request.getActors());
        if (request.getLanguage() != null) movie.setLanguage(request.getLanguage());
        if (request.getSubtitleLanguage() != null) movie.setSubtitleLanguage(request.getSubtitleLanguage());
        if (request.getCountry() != null) movie.setCountry(request.getCountry());
        if (request.getAgeRating() != null) movie.setAgeRating(request.getAgeRating());
        if (request.getTrailerUrl() != null) movie.setTrailerUrl(request.getTrailerUrl());
        if (request.getRatingImdb() != null) movie.setRatingImdb(request.getRatingImdb());
    }
}
