package com.cinema.booking.mapper;

import com.cinema.booking.dto.response.TicketResponse;
import com.cinema.booking.entity.Ticket;
import com.cinema.booking.entity.User;
import com.cinema.booking.service.QrCodeImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TicketMapper {

    private final QrCodeImageService qrCodeImageService;

    public TicketResponse toTicketResponse(Ticket ticket) {
        return toTicketResponse(ticket, false);
    }

    public TicketResponse toTicketResponse(Ticket ticket, boolean alreadyCheckedIn) {
        var bd = ticket.getBookingDetail();
        var showtime = bd.getBooking().getShowtime();
        User checkedInBy = ticket.getCheckedInBy();

        return TicketResponse.builder()
                .id(ticket.getId())
                .qrCode(ticket.getQrCode())
                .qrImage(qrCodeImageService.toPngDataUri(ticket.getQrCode(), 360))
                .status(ticket.getStatus())
                .checkInTime(ticket.getCheckInTime())
                .checkedInById(checkedInBy != null ? checkedInBy.getId() : null)
                .checkedInByUsername(checkedInBy != null ? checkedInBy.getUsername() : null)
                .checkedInByName(resolveDisplayName(checkedInBy))
                .alreadyCheckedIn(alreadyCheckedIn)
                .movieTitle(showtime.getMovie().getTitle())
                .cinemaName(showtime.getRoom().getCinema().getName())
                .roomName(showtime.getRoom().getName())
                .startTime(showtime.getStartTime())
                .rowLabel(bd.getSeat().getRowLabel())
                .seatNumber(bd.getSeat().getSeatNumber())
                .seatType(bd.getSeat().getSeatType().name())
                .build();
    }

    private String resolveDisplayName(User user) {
        if (user == null) {
            return null;
        }

        String fullName = ((user.getFirstName() != null ? user.getFirstName() : "") + " "
                + (user.getLastName() != null ? user.getLastName() : "")).trim();
        return fullName.isBlank() ? user.getUsername() : fullName;
    }
}
