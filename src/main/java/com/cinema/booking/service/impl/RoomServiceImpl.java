package com.cinema.booking.service.impl;

import com.cinema.booking.dto.request.RoomCreationRequest;
import com.cinema.booking.dto.request.RoomUpdateRequest;
import com.cinema.booking.dto.response.RoomResponse;
import com.cinema.booking.entity.Cinema;
import com.cinema.booking.entity.Room;
import com.cinema.booking.enums.ErrorCode;
import com.cinema.booking.exception.AppException;
import com.cinema.booking.mapper.RoomMapper;
import com.cinema.booking.repository.CinemaRepository;
import com.cinema.booking.repository.RoomRepository;
import com.cinema.booking.service.RoomService;
import com.cinema.booking.service.StaffCinemaScopeService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class RoomServiceImpl implements RoomService {

    RoomRepository roomRepository;
    CinemaRepository cinemaRepository;
    RoomMapper roomMapper;
    StaffCinemaScopeService staffCinemaScopeService;

    @Override
    @Transactional
    public RoomResponse createRoom(RoomCreationRequest request) {
        Cinema cinema = cinemaRepository.findActiveById(request.getCinemaId())
                .orElseThrow(() -> new AppException(ErrorCode.CINEMA_NOT_FOUND));
        staffCinemaScopeService.validateCurrentStaffCanAccessCinema(cinema.getId());

        if (roomRepository.existsByCinemaIdAndNameAndIsDeletedFalse(cinema.getId(), request.getName())) {
            throw new AppException(ErrorCode.ROOM_NAME_EXISTED);
        }

        Room room = roomMapper.toRoom(request, cinema);
        Room saved = roomRepository.save(room);
        log.info("Created new room: {} in cinema: {}", saved.getName(), cinema.getName());
        return roomMapper.toRoomResponse(saved);
    }

    @Override
    @Transactional
    public RoomResponse updateRoom(UUID id, RoomUpdateRequest request) {
        Room room = findActiveRoomById(id);
        staffCinemaScopeService.validateCurrentStaffCanAccessCinema(room.getCinema().getId());

        if (request.getName() != null && !request.getName().equals(room.getName())) {
            if (roomRepository.existsByCinemaIdAndNameAndIsDeletedFalse(room.getCinema().getId(), request.getName())) {
                throw new AppException(ErrorCode.ROOM_NAME_EXISTED);
            }
        }

        roomMapper.updateRoom(room, request);
        Room saved = roomRepository.save(room);
        log.info("Updated room id={}", id);
        return roomMapper.toRoomResponse(saved);
    }

    @Override
    @Transactional
    public void deleteRoom(UUID id) {
        Room room = findActiveRoomById(id);
        staffCinemaScopeService.validateCurrentStaffCanAccessCinema(room.getCinema().getId());
        room.setIsDeleted(true);
        roomRepository.save(room);
        log.info("Soft-deleted room id={}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public RoomResponse getRoomById(UUID id) {
        Room room = findActiveRoomById(id);
        staffCinemaScopeService.validateCurrentStaffCanAccessCinema(room.getCinema().getId());
        return roomMapper.toRoomResponse(room);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomResponse> getRoomsByCinemaId(UUID cinemaId) {
        if (cinemaRepository.findActiveById(cinemaId).isEmpty()) {
            throw new AppException(ErrorCode.CINEMA_NOT_FOUND);
        }
        staffCinemaScopeService.validateCurrentStaffCanAccessCinema(cinemaId);
        
        return roomRepository.findAllByCinemaIdAndIsDeletedFalse(cinemaId)
                .stream()
                .map(roomMapper::toRoomResponse)
                .collect(Collectors.toList());
    }

    private Room findActiveRoomById(UUID id) {
        return roomRepository.findActiveById(id)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));
    }
}
