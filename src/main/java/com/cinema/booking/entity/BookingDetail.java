package com.cinema.booking.entity;

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
@Table(name = "booking_details")
public class BookingDetail extends BaseEntity {

    @Id
    @GeneratedValue
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_id", nullable = false)
    Seat seat;

    // Lưu giá tại thời điểm booking để đảm bảo dữ liệu lịch sử không thay đổi
    @Column(nullable = false, precision = 10, scale = 2)
    BigDecimal priceAtBooking;

    // Mối quan hệ 1-1 với Ticket, cascade để tạo ticket khi booking thành công
    @OneToOne(mappedBy = "bookingDetail", cascade = CascadeType.ALL, orphanRemoval = true)
    Ticket ticket;
}
