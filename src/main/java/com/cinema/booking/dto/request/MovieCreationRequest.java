package com.cinema.booking.dto.request;

import com.cinema.booking.enums.MovieStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MovieCreationRequest {

    @NotBlank(message = "MOVIE_TITLE_REQUIRED")
    String title;

    String description;

    @NotNull(message = "MOVIE_DURATION_REQUIRED")
    @Min(value = 1, message = "MOVIE_DURATION_INVALID")
    Integer duration;

    String genre;

    @NotNull(message = "MOVIE_RELEASE_DATE_REQUIRED")
    LocalDate releaseDate;

    String posterUrl;

    @NotNull(message = "MOVIE_STATUS_REQUIRED")
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
