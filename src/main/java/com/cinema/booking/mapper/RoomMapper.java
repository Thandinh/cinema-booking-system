package com.cinema.booking.mapper;

import com.cinema.booking.dto.request.RoomCreationRequest;
import com.cinema.booking.dto.request.RoomUpdateRequest;
import com.cinema.booking.dto.response.RoomResponse;
import com.cinema.booking.entity.Cinema;
import com.cinema.booking.entity.Room;
import org.springframework.stereotype.Component;

@Component
public class RoomMapper {

    public Room toRoom(RoomCreationRequest request, Cinema cinema) {
        if (request == null || cinema == null) return null;
        return Room.builder()
                .cinema(cinema)
                .name(request.getName())
                .isDeleted(false)
                .build();
    }

    public RoomResponse toRoomResponse(Room room) {
        if (room == null) return null;
        return RoomResponse.builder()
                .id(room.getId())
                .cinemaId(room.getCinema() != null ? room.getCinema().getId() : null)
                .cinemaName(room.getCinema() != null ? room.getCinema().getName() : null)
                .name(room.getName())
                .createdAt(room.getCreatedAt())
                .updatedAt(room.getUpdatedAt())
                .build();
    }

    public void updateRoom(Room room, RoomUpdateRequest request) {
        if (request == null) return;
        if (request.getName() != null) room.setName(request.getName());
    }
}
