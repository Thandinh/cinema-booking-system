package com.cinema.booking.dto.request;

import com.cinema.booking.enums.PaymentMethod;
import com.cinema.booking.enums.RefundStatus;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RefundSearchRequest {

    RefundStatus status;
    PaymentMethod method;
    String keyword;
    String city;
    UUID cinemaId;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    LocalDate fromDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    LocalDate toDate;
}
