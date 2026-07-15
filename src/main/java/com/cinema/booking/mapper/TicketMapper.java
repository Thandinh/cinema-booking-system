package com.cinema.booking.mapper;

import com.cinema.booking.dto.response.TicketResponse;
import com.cinema.booking.entity.Ticket;
import com.cinema.booking.service.QrCodeImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TicketMapper {

    private final QrCodeImageService qrCodeImageService;

    public TicketResponse toTicketResponse(Ticket ticket) {
        var bd = ticket.getBookingDetail();
        var showtime = bd.getBooking().getShowtime();

        return TicketResponse.builder()
                .id(ticket.getId())
                .qrCode(ticket.getQrCode())
                .qrImage(qrCodeImageService.toPngDataUri(ticket.getQrCode(), 360))
                .status(ticket.getStatus())
                .checkInTime(ticket.getCheckInTime())
                .movieTitle(showtime.getMovie().getTitle())
                .cinemaName(showtime.getRoom().getCinema().getName())
                .roomName(showtime.getRoom().getName())
                .startTime(showtime.getStartTime())
                .rowLabel(bd.getSeat().getRowLabel())
                .seatNumber(bd.getSeat().getSeatNumber())
                .seatType(bd.getSeat().getSeatType().name())
                .build();
    }
}
