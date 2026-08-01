package com.cinema.booking.dto.request;

import com.cinema.booking.enums.PaymentEventType;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentEventSearchRequest {

    UUID bookingId;
    UUID paymentId;
    PaymentEventType eventType;
    Boolean success;
    String keyword;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    LocalDate fromDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    LocalDate toDate;
}
