package com.cinema.booking.dto.response;

import com.cinema.booking.enums.PaymentMethod;
import com.cinema.booking.enums.PaymentStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentResponse {
    UUID id;
    UUID bookingId;
    String bookingCode;
    String bookingStatus;
    UUID customerId;
    String customerUsername;
    String customerName;
    String customerEmail;
    String movieTitle;
    String cinemaName;
    String roomName;
    LocalDateTime showtimeStartTime;
    BigDecimal amount;
    PaymentMethod method;
    String transactionNo;
    PaymentStatus status;
    LocalDateTime paymentTime;
}
