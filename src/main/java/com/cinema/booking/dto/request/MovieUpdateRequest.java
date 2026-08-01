package com.cinema.booking.dto.request;

import com.cinema.booking.enums.MovieStatus;
import jakarta.validation.constraints.Min;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MovieUpdateRequest {

    String title;

    String description;

    @Min(value = 1, message = "MOVIE_DURATION_INVALID")
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
}
