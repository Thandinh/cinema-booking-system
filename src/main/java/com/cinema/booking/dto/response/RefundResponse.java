package com.cinema.booking.dto.response;

import com.cinema.booking.enums.BookingStatus;
import com.cinema.booking.enums.PaymentMethod;
import com.cinema.booking.enums.PaymentStatus;
import com.cinema.booking.enums.RefundStatus;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RefundResponse {

    UUID id;
    UUID bookingId;
    String bookingCode;
    BookingStatus bookingStatus;
    UUID paymentId;
    PaymentStatus paymentStatus;
    PaymentMethod method;
    String transactionNo;
    BigDecimal amount;
    RefundStatus status;
    String reason;
    String providerRefundId;
    String failureReason;
    LocalDateTime requestedAt;
    LocalDateTime processedAt;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;

    UUID customerId;
    String customerUsername;
    String customerName;
    String customerEmail;

    String movieTitle;
    UUID cinemaId;
    String cinemaName;
    String cinemaCity;
    String roomName;
    LocalDateTime showtimeStartTime;
}
