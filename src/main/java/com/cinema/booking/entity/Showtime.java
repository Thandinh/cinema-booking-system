package com.cinema.booking.entity;

import com.cinema.booking.enums.ShowtimeStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "showtimes")
public class Showtime extends BaseEntity {

    @Id
    @GeneratedValue
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id", nullable = false)
    Movie movie;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    Room room;

    @Column(nullable = false)
    LocalDateTime startTime;

    @Column(nullable = false)
    LocalDateTime endTime;

    @Column(nullable = false, precision = 10, scale = 2)
    BigDecimal basePrice;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    ShowtimeStatus status = ShowtimeStatus.UPCOMING;

    @Builder.Default
    Boolean isDeleted = false;
}
