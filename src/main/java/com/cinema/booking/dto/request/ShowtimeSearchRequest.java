package com.cinema.booking.dto.request;

import com.cinema.booking.enums.ShowtimeStatus;
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
public class ShowtimeSearchRequest {

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    LocalDate fromDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    LocalDate toDate;

    String city;
    UUID cinemaId;
    UUID roomId;
    ShowtimeStatus status;
    String keyword;
}
