package com.cinema.booking.service.impl;

import com.cinema.booking.dto.request.CinemaCreationRequest;
import com.cinema.booking.dto.request.CinemaUpdateRequest;
import com.cinema.booking.dto.response.CinemaResponse;
import com.cinema.booking.entity.Cinema;
import com.cinema.booking.enums.ErrorCode;
import com.cinema.booking.exception.AppException;
import com.cinema.booking.mapper.CinemaMapper;
import com.cinema.booking.repository.CinemaRepository;
import com.cinema.booking.repository.RoomRepository;
import com.cinema.booking.repository.SeatRepository;
import com.cinema.booking.service.CinemaService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class CinemaServiceImpl implements CinemaService {

    CinemaRepository cinemaRepository;
    RoomRepository   roomRepository;
    SeatRepository   seatRepository;
    CinemaMapper     cinemaMapper;

    @Override
    @Transactional
    public CinemaResponse createCinema(CinemaCreationRequest request) {
        if (cinemaRepository.existsByNameAndIsDeletedFalse(request.getName())) {
            throw new AppException(ErrorCode.CINEMA_NAME_EXISTED);
        }
        Cinema saved = cinemaRepository.save(cinemaMapper.toCinema(request));
        log.info("Created cinema: {}", saved.getName());
        return cinemaMapper.toCinemaResponse(saved, false);
    }

    @Override
    @Transactional
    public CinemaResponse updateCinema(UUID id, CinemaUpdateRequest request) {
        Cinema cinema = findActiveCinemaById(id);

        if (request.getName() != null && !request.getName().equals(cinema.getName())
                && cinemaRepository.existsByNameAndIsDeletedFalse(request.getName())) {
            throw new AppException(ErrorCode.CINEMA_NAME_EXISTED);
        }

        cinemaMapper.updateCinema(cinema, request);
        log.info("Updated cinema id={}", id);
        return cinemaMapper.toCinemaResponse(cinemaRepository.save(cinema), false);
    }

    /**
     * Soft-delete Cinema + cascade soft-delete toàn bộ Rooms và Seats thuộc về rạp.
     * Thực hiện bằng bulk UPDATE (2-3 queries) thay vì load từng entity.
     */
    @Override
    @Transactional
    public void deleteCinema(UUID id) {
        Cinema cinema = findActiveCinemaById(id);

        // 1. Lấy danh sách room IDs để cascade xoá ghế
        List<UUID> roomIds = roomRepository.findActiveRoomIdsByCinemaId(id);

        // 2. Soft-delete tất cả Seats trong các phòng đó (bulk)
        if (!roomIds.isEmpty()) {
            roomIds.forEach(seatRepository::softDeleteByRoomId);
        }

        // 3. Soft-delete tất cả Rooms của rạp (bulk)
        roomRepository.softDeleteByCinemaId(id);

        // 4. Soft-delete Cinema
        cinema.setIsDeleted(true);
        cinemaRepository.save(cinema);

        log.info("Soft-deleted cinema id={} (cascaded {} rooms)", id, roomIds.size());
    }

    @Override
    @Transactional(readOnly = true)
    public CinemaResponse getCinemaById(UUID id) {
        return cinemaMapper.toCinemaResponse(findActiveCinemaById(id), true);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CinemaResponse> getAllCinemas(Pageable pageable, boolean onlyActive) {
        Page<Cinema> page = onlyActive
                ? cinemaRepository.findAllByIsActiveTrueAndIsDeletedFalse(pageable)
                : cinemaRepository.findAllByIsDeletedFalse(pageable);
        return page.map(c -> cinemaMapper.toCinemaResponse(c, false));
    }

    private Cinema findActiveCinemaById(UUID id) {
        return cinemaRepository.findActiveById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CINEMA_NOT_FOUND));
    }
}
