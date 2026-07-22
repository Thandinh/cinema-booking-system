package com.cinema.booking.entity;

import com.cinema.booking.enums.TicketStatus;
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
@Table(name = "tickets")
public class Ticket extends BaseEntity {

    @Id
    @GeneratedValue
    UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_detail_id", nullable = false)
    BookingDetail bookingDetail;

    @Column(nullable = false, unique = true, length = 100)
    String qrCode;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    TicketStatus status = TicketStatus.ACTIVE;

    LocalDateTime checkInTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checked_in_by")
    User checkedInBy;
}
