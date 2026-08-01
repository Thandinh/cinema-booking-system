package com.cinema.booking.controller;

import com.cinema.booking.dto.request.SeatBulkGenerateRequest;
import com.cinema.booking.dto.request.SeatCreationRequest;
import com.cinema.booking.dto.request.SeatUpdateRequest;
import com.cinema.booking.dto.response.ApiResponse;
import com.cinema.booking.dto.response.SeatBulkGenerateResponse;
import com.cinema.booking.dto.response.SeatResponse;
import com.cinema.booking.service.SeatService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/seats")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SeatController {

    SeatService seatService;

    /**
     * Tạo 1 ghế đơn lẻ.
     * Dùng khi cần thêm ghế đặc biệt (ví dụ: ghế cho người khuyết tật)
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('SEAT_CREATE')")
    public ApiResponse<SeatResponse> createSeat(@Valid @RequestBody SeatCreationRequest request) {
        return ApiResponse.<SeatResponse>builder()
                .code(1000)
                .message("Seat created successfully")
                .result(seatService.createSeat(request))
                .build();
    }

    /**
     * Tự động sinh toàn bộ sơ đồ ghế cho một phòng chiếu.
     * Ví dụ: rows A-E, 10 ghế/hàng → sinh 50 ghế tự động có rowIndex/colIndex.
     */
    @PostMapping("/bulk-generate")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('SEAT_CREATE')")
    public ApiResponse<SeatBulkGenerateResponse> bulkGenerateSeats(@Valid @RequestBody SeatBulkGenerateRequest request) {
        return ApiResponse.<SeatBulkGenerateResponse>builder()
                .code(1000)
                .message("Seats generated successfully")
                .result(seatService.bulkGenerateSeats(request))
                .build();
    }

    /**
     * Lấy toàn bộ ghế của một phòng chiếu.
     * Trả về đã sắp xếp rowLabel → seatNumber.
     */
    @GetMapping("/room/{roomId}")
    @PreAuthorize("hasAuthority('SEAT_VIEW')")
    public ApiResponse<List<SeatResponse>> getSeatsByRoom(@PathVariable UUID roomId) {
        return ApiResponse.<List<SeatResponse>>builder()
                .code(1000)
                .result(seatService.getSeatsByRoomId(roomId))
                .build();
    }

    /** Lấy chi tiết 1 ghế */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SEAT_VIEW')")
    public ApiResponse<SeatResponse> getSeatById(@PathVariable UUID id) {
        return ApiResponse.<SeatResponse>builder()
                .code(1000)
                .result(seatService.getSeatById(id))
                .build();
    }

    /** Cập nhật loại ghế / hệ số giá / vị trí grid */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SEAT_UPDATE')")
    public ApiResponse<SeatResponse> updateSeat(
            @PathVariable UUID id,
            @Valid @RequestBody SeatUpdateRequest request) {
        return ApiResponse.<SeatResponse>builder()
                .code(1000)
                .message("Seat updated successfully")
                .result(seatService.updateSeat(id, request))
                .build();
    }

    /** Xoá mềm ghế */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SEAT_DELETE')")
    public ApiResponse<Void> deleteSeat(@PathVariable UUID id) {
        seatService.deleteSeat(id);
        return ApiResponse.<Void>builder()
                .code(1000)
                .message("Seat deleted successfully")
                .build();
    }
}
