package com.cinema.booking.mapper;

import com.cinema.booking.dto.request.CinemaCreationRequest;
import com.cinema.booking.dto.request.CinemaUpdateRequest;
import com.cinema.booking.dto.response.CinemaResponse;
import com.cinema.booking.entity.Cinema;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CinemaMapper {

    private final RoomMapper roomMapper;

    public Cinema toCinema(CinemaCreationRequest request) {
        if (request == null) return null;
        return Cinema.builder()
                .name(request.getName())
                .address(request.getAddress())
                .city(request.getCity())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .isActive(true)
                .isDeleted(false)
                .build();
    }

    public CinemaResponse toCinemaResponse(Cinema cinema, boolean includeRooms) {
        if (cinema == null) return null;
        
        CinemaResponse.CinemaResponseBuilder builder = CinemaResponse.builder()
                .id(cinema.getId())
                .name(cinema.getName())
                .address(cinema.getAddress())
                .city(cinema.getCity())
                .latitude(cinema.getLatitude())
                .longitude(cinema.getLongitude())
                .isActive(cinema.getIsActive())
                .createdAt(cinema.getCreatedAt())
                .updatedAt(cinema.getUpdatedAt());

        if (includeRooms && cinema.getRooms() != null) {
            builder.rooms(cinema.getRooms().stream()
                    .filter(room -> !room.getIsDeleted())
                    .map(roomMapper::toRoomResponse)
                    .collect(Collectors.toList()));
        }

        return builder.build();
    }

    public void updateCinema(Cinema cinema, CinemaUpdateRequest request) {
        if (request == null) return;
        if (request.getName() != null) cinema.setName(request.getName());
        if (request.getAddress() != null) cinema.setAddress(request.getAddress());
        if (request.getCity() != null) cinema.setCity(request.getCity());
        if (request.getLatitude() != null) cinema.setLatitude(request.getLatitude());
        if (request.getLongitude() != null) cinema.setLongitude(request.getLongitude());
        if (request.getIsActive() != null) cinema.setIsActive(request.getIsActive());
    }
}
