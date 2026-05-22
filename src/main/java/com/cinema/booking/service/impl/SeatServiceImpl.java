package com.cinema.booking.service.impl;

import com.cinema.booking.dto.request.SeatBulkGenerateRequest;
import com.cinema.booking.dto.request.SeatCreationRequest;
import com.cinema.booking.dto.request.SeatUpdateRequest;
import com.cinema.booking.dto.response.SeatBulkGenerateResponse;
import com.cinema.booking.dto.response.SeatResponse;
import com.cinema.booking.entity.Room;
import com.cinema.booking.entity.Seat;
import com.cinema.booking.enums.ErrorCode;
import com.cinema.booking.enums.SeatType;
import com.cinema.booking.exception.AppException;
import com.cinema.booking.mapper.SeatMapper;
import com.cinema.booking.repository.RoomRepository;
import com.cinema.booking.repository.SeatRepository;
import com.cinema.booking.service.SeatService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
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

    // ─────────────────────────────────────────────────────────────────────────
    // CREATE SINGLE
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public SeatResponse createSeat(SeatCreationRequest request) {
        Room room = findActiveRoom(request.getRoomId());

        String rowLabel = request.getRowLabel().toUpperCase();
        if (seatRepository.existsByRoomIdAndRowLabelAndSeatNumber(room.getId(), rowLabel, request.getSeatNumber())) {
            throw new AppException(ErrorCode.SEAT_ALREADY_EXISTS);
        }

        Seat seat = seatMapper.toSeat(request, room);
        Seat saved = seatRepository.save(seat);
        log.info("Created seat {} in room {}", saved.getRowLabel() + saved.getSeatNumber(), room.getName());
        return seatMapper.toSeatResponse(saved);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BULK GENERATE — tính rowIndex / colIndex tự động
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public SeatBulkGenerateResponse bulkGenerateSeats(SeatBulkGenerateRequest request) {
        Room room = findActiveRoom(request.getRoomId());

        List<String>  rowLabels   = request.getRowLabels();
        int           seatsPerRow = request.getSeatsPerRow();
        SeatType      seatType    = request.getSeatType() != null       ? request.getSeatType()       : SeatType.NORMAL;
        BigDecimal    multiplier  = request.getPriceMultiplier() != null ? request.getPriceMultiplier() : BigDecimal.ONE;

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

                toSave.add(Seat.builder()
                        .room(room)
                        .rowLabel(rowLabel)
                        .seatNumber(seatNumber)
                        .seatType(seatType)
                        .priceMultiplier(multiplier)
                        .rowIndex(rowIdx)   // 0-based for frontend grid
                        .colIndex(colIdx)   // 0-based for frontend grid
                        .isDeleted(false)
                        .build());
            }
        }

        List<Seat> saved = seatRepository.saveAll(toSave);
        log.info("Bulk generated {} seats in room '{}' (skipped: {})", saved.size(), room.getName(), totalSkipped);

        return SeatBulkGenerateResponse.builder()
                .totalRequested(totalRequested)
                .totalCreated(saved.size())
                .totalSkipped(totalSkipped)
                .createdSeats(saved.stream().map(seatMapper::toSeatResponse).collect(Collectors.toList()))
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UPDATE
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public SeatResponse updateSeat(UUID id, SeatUpdateRequest request) {
        Seat seat = findActiveSeat(id);
        seatMapper.updateSeat(seat, request);
        return seatMapper.toSeatResponse(seatRepository.save(seat));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DELETE (soft)
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void deleteSeat(UUID id) {
        Seat seat = findActiveSeat(id);
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
        return seatMapper.toSeatResponse(findActiveSeat(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SeatResponse> getSeatsByRoomId(UUID roomId) {
        if (roomRepository.findActiveById(roomId).isEmpty()) {
            throw new AppException(ErrorCode.ROOM_NOT_FOUND);
        }
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
}
