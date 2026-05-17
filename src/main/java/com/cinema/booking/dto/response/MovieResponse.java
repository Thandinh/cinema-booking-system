package com.cinema.booking.dto.response;

import com.cinema.booking.enums.MovieStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MovieResponse {
    UUID id;
    String title;
    String description;
    Integer duration;
    String genre;
    LocalDate releaseDate;
    String posterUrl;
    MovieStatus status;
    String director;
    String actors;
    String language;
    String subtitleLanguage;
    String country;
    String ageRating;
    String trailerUrl;
    BigDecimal ratingImdb;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
