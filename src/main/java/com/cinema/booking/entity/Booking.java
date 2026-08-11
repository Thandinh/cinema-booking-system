package com.cinema.booking.entity;

import com.cinema.booking.enums.BookingStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "bookings")
public class Booking extends BaseEntity {

    @Id
    @GeneratedValue
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "showtime_id", nullable = false)
    Showtime showtime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion_id")
    Promotion promotion;

    @Column(nullable = false, precision = 10, scale = 2)
    BigDecimal totalPrice;

    @Column(precision = 10, scale = 2)
    @Builder.Default
    BigDecimal discountAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    BookingStatus status = BookingStatus.PENDING;

    // Token bảo mật dùng để xác thực callback thanh toán từ VNPay/MoMo
    @Column(unique = true, nullable = false, length = 255)
    String secureToken;

    LocalDateTime paymentExpiresAt;

    /**
     * A promotion slot is reserved when a pending booking is created and is
     * released only when that booking cannot complete. Keeping this state on
     * the booking makes a promotion usage limit safe under concurrent payment.
     */
    @Builder.Default
    boolean promotionReserved = false;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    List<BookingDetail> bookingDetails = new ArrayList<>();
}
