package com.cinema.booking.entity;

import com.cinema.booking.enums.MovieStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "movies")
public class Movie extends BaseEntity {

    @Id
    @GeneratedValue
    UUID id;

    @Column(nullable = false)
    String title;

    @Column(columnDefinition = "TEXT")
    String description;

    Integer duration; // Thời lượng phim (phút)

    String genre; // Thể loại

    LocalDate releaseDate; // Ngày công chiếu

    @Column(columnDefinition = "TEXT")
    String posterUrl;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    MovieStatus status;

    String director;

    @Column(columnDefinition = "TEXT")
    String actors;

    String language;

    String subtitleLanguage;

    String country;

    String ageRating;

    @Column(columnDefinition = "TEXT")
    String trailerUrl;

    @Column(precision = 3, scale = 1)
    BigDecimal ratingImdb;

    @Builder.Default
    Boolean isDeleted = false;
}
