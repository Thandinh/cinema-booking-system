package com.cinema.booking.dto.request;

import com.cinema.booking.enums.PaymentMethod;
import com.cinema.booking.enums.PaymentStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentSearchRequest {

    PaymentStatus status;
    PaymentMethod method;
    String keyword;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    LocalDate fromDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    LocalDate toDate;
}
