package com.cinema.booking.mapper;

import com.cinema.booking.dto.request.SeatCreationRequest;
import com.cinema.booking.dto.request.SeatUpdateRequest;
import com.cinema.booking.dto.response.SeatResponse;
import com.cinema.booking.entity.Room;
import com.cinema.booking.entity.Seat;
import com.cinema.booking.enums.SeatType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class SeatMapper {

    public Seat toSeat(SeatCreationRequest request, Room room) {
        return Seat.builder()
                .room(room)
                .rowLabel(request.getRowLabel().toUpperCase())
                .seatNumber(request.getSeatNumber())
                .seatType(request.getSeatType() != null ? request.getSeatType() : SeatType.NORMAL)
                .priceMultiplier(request.getPriceMultiplier() != null ? request.getPriceMultiplier() : BigDecimal.ONE)
                .rowIndex(request.getRowIndex())
                .colIndex(request.getColIndex())
                .isDeleted(false)
                .build();
    }

    public SeatResponse toSeatResponse(Seat seat) {
        return SeatResponse.builder()
                .id(seat.getId())
                .roomId(seat.getRoom() != null ? seat.getRoom().getId() : null)
                .roomName(seat.getRoom() != null ? seat.getRoom().getName() : null)
                .rowLabel(seat.getRowLabel())
                .seatNumber(seat.getSeatNumber())
                .seatCode(seat.getRowLabel() + seat.getSeatNumber())   // e.g. "A5"
                .seatType(seat.getSeatType())
                .priceMultiplier(seat.getPriceMultiplier())
                .rowIndex(seat.getRowIndex())
                .colIndex(seat.getColIndex())
                .build();
    }

    public void updateSeat(Seat seat, SeatUpdateRequest request) {
        if (request.getSeatType() != null)        seat.setSeatType(request.getSeatType());
        if (request.getPriceMultiplier() != null) seat.setPriceMultiplier(request.getPriceMultiplier());
        if (request.getRowIndex() != null)        seat.setRowIndex(request.getRowIndex());
        if (request.getColIndex() != null)        seat.setColIndex(request.getColIndex());
    }
}
