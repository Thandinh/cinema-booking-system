package com.cinema.booking.service.impl;

import com.cinema.booking.dto.request.SeatBulkGenerateRequest;
import com.cinema.booking.dto.request.SeatCreationRequest;
import com.cinema.booking.dto.request.SeatUpdateRequest;
import com.cinema.booking.dto.response.SeatBulkGenerateResponse;
import com.cinema.booking.dto.response.SeatResponse;
import com.cinema.booking.configuration.CacheConfig;
import com.cinema.booking.entity.Room;
import com.cinema.booking.entity.Seat;
import com.cinema.booking.enums.ErrorCode;
import com.cinema.booking.enums.SeatLayoutTemplate;
import com.cinema.booking.enums.SeatType;
import com.cinema.booking.exception.AppException;
import com.cinema.booking.mapper.SeatMapper;
import com.cinema.booking.repository.RoomRepository;
import com.cinema.booking.repository.SeatRepository;
import com.cinema.booking.service.SeatService;
import com.cinema.booking.service.StaffCinemaScopeService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class SeatServiceImpl implements SeatService {

    SeatRepository seatRepository;
    RoomRepository roomRepository;
    SeatMapper seatMapper;
    com.cinema.booking.repository.SeatStatusRepository seatStatusRepository;
    StaffCinemaScopeService staffCinemaScopeService;

    // ─────────────────────────────────────────────────────────────────────────
    // CREATE SINGLE
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheConfig.SEATS_BY_ROOM, allEntries = true)
    public SeatResponse createSeat(SeatCreationRequest request) {
        Room room = findActiveRoom(request.getRoomId());
        validateRoomScope(room);

        String rowLabel = request.getRowLabel().toUpperCase();
        if (seatRepository.existsByRoomIdAndRowLabelAndSeatNumber(room.getId(), rowLabel, request.getSeatNumber())) {
            throw new AppException(ErrorCode.SEAT_ALREADY_EXISTS);
        }

        Seat seat = seatMapper.toSeat(request, room);
        Seat saved = seatRepository.save(seat);
        int syncedSeatStatuses = syncSeatsToFutureShowtimes(room.getId(), List.of(saved.getId()));
        log.info("Created seat {} in room {} and synced {} future seat_status rows",
                saved.getRowLabel() + saved.getSeatNumber(), room.getName(), syncedSeatStatuses);
        return seatMapper.toSeatResponse(saved);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BULK GENERATE — tính rowIndex / colIndex tự động
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheConfig.SEATS_BY_ROOM, allEntries = true)
    public SeatBulkGenerateResponse bulkGenerateSeats(SeatBulkGenerateRequest request) {
        Room room = findActiveRoom(request.getRoomId());
        validateRoomScope(room);

        List<String>  rowLabels   = normalizeRowLabels(request.getRowLabels());
        int           seatsPerRow = request.getSeatsPerRow();
        SeatLayoutTemplate layoutTemplate = request.getLayoutTemplate() != null
                ? request.getLayoutTemplate()
                : SeatLayoutTemplate.CUSTOM;
        SeatType      seatType    = request.getSeatType() != null ? request.getSeatType() : SeatType.NORMAL;
        BigDecimal    multiplier  = request.getPriceMultiplier() != null
                ? request.getPriceMultiplier()
                : defaultMultiplier(seatType);

        // ✅ Load toàn bộ existing seat keys trong 1 query duy nhất
        // Format: "A:1", "A:2", "B:1" ... tránh N+1 EXISTS queries trong loop
        Set<String> existingKeys = seatRepository.findSeatKeysByRoomId(room.getId());

        int totalRequested = rowLabels.size() * seatsPerRow;
        int totalSkipped   = 0;
        List<Seat> toSave  = new ArrayList<>();

        for (int rowIdx = 0; rowIdx < rowLabels.size(); rowIdx++) {
            String rowLabel = rowLabels.get(rowIdx).toUpperCase();

            for (int colIdx = 0; colIdx < seatsPerRow; colIdx++) {
                int seatNumber = colIdx + 1;  // 1-based
                String key = rowLabel + ":" + seatNumber;

                if (existingKeys.contains(key)) {
                    if (request.isSkipExisting()) {
                        totalSkipped++;
                        continue;
                    } else {
                        throw new AppException(ErrorCode.SEAT_ALREADY_EXISTS);
                    }
                }

                SeatType rowSeatType = resolveSeatType(layoutTemplate, rowIdx, rowLabels.size(), seatType);
                BigDecimal rowMultiplier = layoutTemplate == SeatLayoutTemplate.STANDARD_CINEMA
                        ? defaultMultiplier(rowSeatType)
                        : multiplier;

                toSave.add(Seat.builder()
                        .room(room)
                        .rowLabel(rowLabel)
                        .seatNumber(seatNumber)
                        .seatType(rowSeatType)
                        .priceMultiplier(rowMultiplier)
                        .rowIndex(rowIdx)   // 0-based for frontend grid
                        .colIndex(colIdx)   // 0-based for frontend grid
                        .isDeleted(false)
                        .build());
            }
        }

        List<Seat> saved = seatRepository.saveAll(toSave);
        List<UUID> savedSeatIds = saved.stream().map(Seat::getId).toList();
        int syncedSeatStatuses = syncSeatsToFutureShowtimes(room.getId(), savedSeatIds);
        log.info("Bulk generated {} seats in room '{}' (skipped: {}, synced seat_status: {})",
                saved.size(), room.getName(), totalSkipped, syncedSeatStatuses);

        return SeatBulkGenerateResponse.builder()
                .totalRequested(totalRequested)
                .totalCreated(saved.size())
                .totalSkipped(totalSkipped)
                .totalSeatStatusesCreated(syncedSeatStatuses)
                .createdSeats(saved.stream().map(seatMapper::toSeatResponse).collect(Collectors.toList()))
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UPDATE
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheConfig.SEATS_BY_ROOM, allEntries = true)
    public SeatResponse updateSeat(UUID id, SeatUpdateRequest request) {
        Seat seat = findActiveSeat(id);
        validateSeatScope(seat);
        seatMapper.updateSeat(seat, request);
        return seatMapper.toSeatResponse(seatRepository.save(seat));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DELETE (soft)
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheConfig.SEATS_BY_ROOM, allEntries = true)
    public void deleteSeat(UUID id) {
        Seat seat = findActiveSeat(id);
        validateSeatScope(seat);
        
        boolean isInUse = seatStatusRepository.existsBySeatIdAndStatusIn(
                id, 
                List.of(com.cinema.booking.enums.SeatStatusType.HOLD, com.cinema.booking.enums.SeatStatusType.BOOKED)
        );
        if (isInUse) {
            throw new AppException(ErrorCode.SEAT_IN_USE);
        }

        seat.setIsDeleted(true);
        seatRepository.save(seat);
        log.info("Soft-deleted seat id={}", id);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // READ
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public SeatResponse getSeatById(UUID id) {
        Seat seat = findActiveSeat(id);
        validateSeatScope(seat);
        return seatMapper.toSeatResponse(seat);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SeatResponse> getSeatsByRoomId(UUID roomId) {
        Room room = findActiveRoom(roomId);
        validateRoomScope(room);
        return seatRepository.findActiveByRoomId(roomId)
                .stream()
                .map(seatMapper::toSeatResponse)
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private Room findActiveRoom(UUID roomId) {
        return roomRepository.findActiveById(roomId)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));
    }

    private Seat findActiveSeat(UUID id) {
        return seatRepository.findActiveById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SEAT_NOT_FOUND));
    }

    private void validateRoomScope(Room room) {
        staffCinemaScopeService.validateCurrentStaffCanAccessCinema(room.getCinema().getId());
    }

    private void validateSeatScope(Seat seat) {
        validateRoomScope(seat.getRoom());
    }

    private List<String> normalizeRowLabels(List<String> rowLabels) {
        return rowLabels.stream()
                .map(label -> label == null ? "" : label.trim().toUpperCase())
                .filter(label -> !label.isBlank())
                .collect(Collectors.collectingAndThen(
                        Collectors.toCollection(LinkedHashSet::new),
                        ArrayList::new
                ));
    }

    private int syncSeatsToFutureShowtimes(UUID roomId, List<UUID> seatIds) {
        if (seatIds == null || seatIds.isEmpty()) {
            return 0;
        }
        return seatStatusRepository.insertMissingAvailableForFutureShowtimes(
                roomId,
                seatIds,
                LocalDateTime.now()
        );
    }

    private SeatType resolveSeatType(
            SeatLayoutTemplate layoutTemplate,
            int rowIndex,
            int totalRows,
            SeatType fallbackSeatType
    ) {
        if (layoutTemplate != SeatLayoutTemplate.STANDARD_CINEMA || totalRows < 4) {
            return fallbackSeatType;
        }
        if (rowIndex == totalRows - 1) {
            return SeatType.COUPLE;
        }
        if (rowIndex >= totalRows - 3) {
            return SeatType.VIP;
        }
        return SeatType.NORMAL;
    }

    private BigDecimal defaultMultiplier(SeatType seatType) {
        return switch (seatType) {
            case VIP -> BigDecimal.valueOf(1.5);
            case COUPLE -> BigDecimal.valueOf(1.8);
            case NORMAL -> BigDecimal.ONE;
        };
    }
}
