package com.cinema.booking.controller;

import com.cinema.booking.dto.request.ShowtimeCreationRequest;
import com.cinema.booking.dto.request.ShowtimeCancelRequest;
import com.cinema.booking.dto.request.ShowtimeSearchRequest;
import com.cinema.booking.dto.request.ShowtimeUpdateRequest;
import com.cinema.booking.dto.response.ApiResponse;
import com.cinema.booking.dto.response.HomeShowtimeFeedResponse;
import com.cinema.booking.dto.response.SeatMapItemResponse;
import com.cinema.booking.dto.response.ShowtimeResponse;
import com.cinema.booking.service.BookingService;
import com.cinema.booking.service.ShowtimeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/showtimes")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Validated
public class ShowtimeController {

    ShowtimeService showtimeService;
    BookingService bookingService;

    @GetMapping("/home")
    public ApiResponse<HomeShowtimeFeedResponse> getHomeShowtimes(
            @RequestParam @NotBlank @Size(max = 100) String city,
            @RequestParam UUID cinemaId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "6") @Min(1) @Max(10) int movieLimit,
            @RequestParam(defaultValue = "4") @Min(1) @Max(8) int showtimeLimit) {
        return ApiResponse.<HomeShowtimeFeedResponse>builder()
                .code(1000)
                .result(showtimeService.getHomeShowtimes(city, cinemaId, date, movieLimit, showtimeLimit))
                .build();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('SHOWTIME_CREATE')")
    public ApiResponse<ShowtimeResponse> createShowtime(@Valid @RequestBody ShowtimeCreationRequest request) {
        return ApiResponse.<ShowtimeResponse>builder()
                .code(1000)
                .message("Showtime created successfully")
                .result(showtimeService.createShowtime(request))
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<ShowtimeResponse> getShowtimeById(@PathVariable UUID id) {
        return ApiResponse.<ShowtimeResponse>builder()
                .code(1000)
                .result(showtimeService.getShowtimeById(id))
                .build();
    }

    @GetMapping("/{id}/seats")
    public ApiResponse<List<SeatMapItemResponse>> getSeatMap(@PathVariable UUID id) {
        return ApiResponse.<List<SeatMapItemResponse>>builder()
                .code(1000)
                .result(bookingService.getSeatMap(id))
                .build();
    }

    @GetMapping
    public ApiResponse<Page<ShowtimeResponse>> getAllShowtimes(
            @ModelAttribute ShowtimeSearchRequest request,
            @PageableDefault(size = 20, sort = "startTime") Pageable pageable) {
        return ApiResponse.<Page<ShowtimeResponse>>builder()
                .code(1000)
                .result(showtimeService.getAllShowtimes(request, pageable))
                .build();
    }

    @GetMapping("/movie/{movieId}")
    public ApiResponse<List<ShowtimeResponse>> getShowtimesByMovieId(@PathVariable UUID movieId) {
        return ApiResponse.<List<ShowtimeResponse>>builder()
                .code(1000)
                .result(showtimeService.getShowtimesByMovieId(movieId))
                .build();
    }

    @GetMapping("/cinema/{cinemaId}")
    public ApiResponse<Page<ShowtimeResponse>> getShowtimesByCinemaId(
            @PathVariable UUID cinemaId,
            @PageableDefault(size = 20, sort = "startTime") Pageable pageable) {
        return ApiResponse.<Page<ShowtimeResponse>>builder()
                .code(1000)
                .result(showtimeService.getShowtimesByCinemaId(cinemaId, pageable))
                .build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SHOWTIME_UPDATE')")
    public ApiResponse<ShowtimeResponse> updateShowtime(
            @PathVariable UUID id,
            @Valid @RequestBody ShowtimeUpdateRequest request) {
        return ApiResponse.<ShowtimeResponse>builder()
                .code(1000)
                .message("Showtime updated successfully")
                .result(showtimeService.updateShowtime(id, request))
                .build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SHOWTIME_DELETE')")
    public ApiResponse<Void> deleteShowtime(@PathVariable UUID id) {
        showtimeService.deleteShowtime(id);
        return ApiResponse.<Void>builder()
                .code(1000)
                .message("Showtime deleted successfully")
                .build();
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('SHOWTIME_UPDATE')")
    public ApiResponse<ShowtimeResponse> cancelShowtime(
            @PathVariable UUID id,
            @Valid @RequestBody ShowtimeCancelRequest request) {
        return ApiResponse.<ShowtimeResponse>builder()
                .code(1000)
                .message("Showtime cancelled successfully")
                .result(showtimeService.cancelShowtimeWithPolicy(id, request))
                .build();
    }
}
