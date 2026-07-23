package com.cinema.booking.dto.response;

import com.cinema.booking.enums.BookingStatus;
import com.cinema.booking.enums.PaymentEventType;
import com.cinema.booking.enums.PaymentMethod;
import com.cinema.booking.enums.PaymentStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentEventResponse {
    UUID id;
    UUID paymentId;
    UUID bookingId;
    PaymentMethod method;
    String transactionNo;
    PaymentEventType eventType;
    PaymentStatus paymentStatusBefore;
    PaymentStatus paymentStatusAfter;
    BookingStatus bookingStatusBefore;
    BookingStatus bookingStatusAfter;
    Boolean success;
    String message;
    Map<String, Object> payload;
    LocalDateTime createdAt;
}
