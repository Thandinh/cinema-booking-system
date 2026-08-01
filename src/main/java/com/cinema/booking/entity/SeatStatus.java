package com.cinema.booking.entity;

import com.cinema.booking.enums.SeatStatusType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "seat_status", uniqueConstraints = @UniqueConstraint(
        name = "unique_seat_showtime",
        columnNames = {"seat_id", "showtime_id"}
))
public class SeatStatus extends BaseEntity {

    @Id
    @GeneratedValue
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_id", nullable = false)
    Seat seat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "showtime_id", nullable = false)
    Showtime showtime;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    SeatStatusType status = SeatStatusType.AVAILABLE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hold_by")
    User holdBy;

    LocalDateTime holdUntil;

    @Version
    @Builder.Default
    Integer version = 0; // Dùng cho Optimistic Locking khi đặt vé
}
