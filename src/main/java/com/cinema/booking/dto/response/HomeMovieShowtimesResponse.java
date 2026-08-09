package com.cinema.booking.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HomeMovieShowtimesResponse {

    UUID movieId;
    String movieTitle;
    String posterUrl;
    String ageRating;
    Integer duration;
    List<HomeShowtimeItemResponse> showtimes;
}
