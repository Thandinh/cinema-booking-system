package com.cinema.booking.controller;

import com.cinema.booking.dto.request.RoomCreationRequest;
import com.cinema.booking.dto.request.RoomUpdateRequest;
import com.cinema.booking.dto.response.ApiResponse;
import com.cinema.booking.dto.response.RoomResponse;
import com.cinema.booking.service.RoomService;
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
@RequestMapping("/api/v1/rooms")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoomController {

    RoomService roomService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ROOM_CREATE')")
    public ApiResponse<RoomResponse> createRoom(@Valid @RequestBody RoomCreationRequest request) {
        return ApiResponse.<RoomResponse>builder()
                .code(1000)
                .message("Room created successfully")
                .result(roomService.createRoom(request))
                .build();
    }

    @GetMapping("/cinema/{cinemaId}")
    @PreAuthorize("hasAuthority('ROOM_VIEW')")
    public ApiResponse<List<RoomResponse>> getRoomsByCinemaId(@PathVariable UUID cinemaId) {
        return ApiResponse.<List<RoomResponse>>builder()
                .code(1000)
                .result(roomService.getRoomsByCinemaId(cinemaId))
                .build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ROOM_VIEW')")
    public ApiResponse<RoomResponse> getRoomById(@PathVariable UUID id) {
        return ApiResponse.<RoomResponse>builder()
                .code(1000)
                .result(roomService.getRoomById(id))
                .build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROOM_UPDATE')")
    public ApiResponse<RoomResponse> updateRoom(
            @PathVariable UUID id,
            @Valid @RequestBody RoomUpdateRequest request) {
        return ApiResponse.<RoomResponse>builder()
                .code(1000)
                .message("Room updated successfully")
                .result(roomService.updateRoom(id, request))
                .build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROOM_DELETE')")
    public ApiResponse<Void> deleteRoom(@PathVariable UUID id) {
        roomService.deleteRoom(id);
        return ApiResponse.<Void>builder()
                .code(1000)
                .message("Room deleted successfully")
                .build();
    }
}
