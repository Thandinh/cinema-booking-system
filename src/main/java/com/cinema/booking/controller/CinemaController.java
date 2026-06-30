package com.cinema.booking.controller;

import com.cinema.booking.dto.request.CinemaCreationRequest;
import com.cinema.booking.dto.request.CinemaUpdateRequest;
import com.cinema.booking.dto.response.ApiResponse;
import com.cinema.booking.dto.response.CinemaMapResponse;
import com.cinema.booking.dto.response.CinemaResponse;
import com.cinema.booking.service.CinemaService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cinemas")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CinemaController {

    CinemaService cinemaService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('CINEMA_CREATE')")
    public ApiResponse<CinemaResponse> createCinema(@Valid @RequestBody CinemaCreationRequest request) {
        return ApiResponse.<CinemaResponse>builder()
                .code(1000)
                .message("Cinema created successfully")
                .result(cinemaService.createCinema(request))
                .build();
    }

    @GetMapping
    @PreAuthorize("hasAuthority('CINEMA_VIEW')")
    public ApiResponse<Page<CinemaResponse>> getAllCinemas(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable,
            @RequestParam(defaultValue = "false") boolean onlyActive) {
        return ApiResponse.<Page<CinemaResponse>>builder()
                .code(1000)
                .result(cinemaService.getAllCinemas(pageable, onlyActive))
                .build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CINEMA_VIEW')")
    public ApiResponse<CinemaResponse> getCinemaById(@PathVariable UUID id) {
        return ApiResponse.<CinemaResponse>builder()
                .code(1000)
                .result(cinemaService.getCinemaById(id))
                .build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('CINEMA_UPDATE')")
    public ApiResponse<CinemaResponse> updateCinema(
            @PathVariable UUID id,
            @Valid @RequestBody CinemaUpdateRequest request) {
        return ApiResponse.<CinemaResponse>builder()
                .code(1000)
                .message("Cinema updated successfully")
                .result(cinemaService.updateCinema(id, request))
                .build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('CINEMA_DELETE')")
    public ApiResponse<Void> deleteCinema(@PathVariable UUID id) {
        cinemaService.deleteCinema(id);
        return ApiResponse.<Void>builder()
                .code(1000)
                .message("Cinema deleted successfully")
                .build();
    }

    // ── MAP ENDPOINTS (public — không cần auth) ───────────────────────────────

    /**
     * Trả về tất cả rạp có tọa độ để render Leaflet markers.
     * PUBLIC: Không cần đăng nhập — cả khách vãng lai cũng dùng được.
     */
    @GetMapping("/map")
    public ApiResponse<List<CinemaMapResponse>> getMapData() {
        return ApiResponse.<List<CinemaMapResponse>>builder()
                .code(1000)
                .result(cinemaService.getMapData())
                .build();
    }

    /**
     * Tìm rạp gần nhất theo tọa độ người dùng (từ browser Geolocation API).
     * PUBLIC: Không cần đăng nhập.
     *
     * @param lat   Vĩ độ (ví dụ: 10.762622)
     * @param lng   Kinh độ (ví dụ: 106.660172)
     * @param limit Số rạp muốn lấy, mặc định 5 (tối đa 20)
     */
    @GetMapping("/nearest")
    public ApiResponse<List<CinemaMapResponse>> getNearestCinemas(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "5") int limit) {
        return ApiResponse.<List<CinemaMapResponse>>builder()
                .code(1000)
                .result(cinemaService.getNearestCinemas(lat, lng, limit))
                .build();
    }
}
