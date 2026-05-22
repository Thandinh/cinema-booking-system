package com.cinema.booking.entity;

import com.cinema.booking.enums.SeatType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "seats",
        uniqueConstraints = @UniqueConstraint(
                name = "unique_seat",
                columnNames = {"room_id", "row_label", "seat_number"}
        ))
public class Seat extends BaseEntity {

    @Id
    @GeneratedValue
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    Room room;

    @Column(nullable = false, length = 10)
    String rowLabel;       // e.g. "A", "B", "C"

    @Column(nullable = false)
    Integer seatNumber;    // e.g. 1, 2, 3

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    SeatType seatType = SeatType.NORMAL;

    @Column(precision = 5, scale = 2)
    @Builder.Default
    BigDecimal priceMultiplier = BigDecimal.ONE;

    Integer rowIndex;      // 0-based grid index for frontend rendering
    Integer colIndex;      // 0-based grid index for frontend rendering

    @Builder.Default
    Boolean isDeleted = false;
}
