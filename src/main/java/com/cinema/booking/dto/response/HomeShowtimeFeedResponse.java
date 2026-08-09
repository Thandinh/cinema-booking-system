package com.cinema.booking.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HomeShowtimeFeedResponse {

    String city;
    LocalDate selectedDate;
    List<LocalDate> availableDates;
    List<HomeMovieShowtimesResponse> movies;
}
