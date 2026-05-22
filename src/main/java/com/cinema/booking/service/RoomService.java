package com.cinema.booking.service;

import com.cinema.booking.dto.request.RoomCreationRequest;
import com.cinema.booking.dto.request.RoomUpdateRequest;
import com.cinema.booking.dto.response.RoomResponse;

import java.util.List;
import java.util.UUID;

public interface RoomService {
    RoomResponse createRoom(RoomCreationRequest request);
    RoomResponse updateRoom(UUID id, RoomUpdateRequest request);
    void deleteRoom(UUID id);
    RoomResponse getRoomById(UUID id);
    List<RoomResponse> getRoomsByCinemaId(UUID cinemaId);
}
