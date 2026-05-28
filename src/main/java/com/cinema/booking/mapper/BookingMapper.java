package com.cinema.booking.mapper;

import com.cinema.booking.dto.response.BookingDetailResponse;
import com.cinema.booking.dto.response.BookingResponse;
import com.cinema.booking.dto.response.SeatMapItemResponse;
import com.cinema.booking.entity.Booking;
import com.cinema.booking.entity.BookingDetail;
import com.cinema.booking.entity.SeatStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class BookingMapper {

    public BookingDetailResponse toBookingDetailResponse(BookingDetail detail) {
        return BookingDetailResponse.builder()
                .id(detail.getId())
                .seatId(detail.getSeat().getId())
                .rowLabel(detail.getSeat().getRowLabel())
                .seatNumber(detail.getSeat().getSeatNumber())
                .seatType(detail.getSeat().getSeatType())
                .priceAtBooking(detail.getPriceAtBooking())
                // QR code chỉ có khi booking thành công và ticket đã được tạo
                .ticketQrCode(detail.getTicket() != null ? detail.getTicket().getQrCode() : null)
                .build();
    }

    public BookingResponse toBookingResponse(Booking booking) {
        List<BookingDetailResponse> detailResponses = booking.getBookingDetails().stream()
                .map(this::toBookingDetailResponse)
                .collect(Collectors.toList());

        return BookingResponse.builder()
                .id(booking.getId())
                .secureToken(booking.getSecureToken())
                .status(booking.getStatus())
                .showtimeId(booking.getShowtime().getId())
                .movieTitle(booking.getShowtime().getMovie().getTitle())
                .cinemaName(booking.getShowtime().getRoom().getCinema().getName())
                .roomName(booking.getShowtime().getRoom().getName())
                .startTime(booking.getShowtime().getStartTime())
                .totalPrice(booking.getTotalPrice())
                .discountAmount(booking.getDiscountAmount())
                .promotionCode(booking.getPromotion() != null ? booking.getPromotion().getCode() : null)
                .bookingDetails(detailResponses)
                .createdAt(booking.getCreatedAt())
                .build();
    }

    public SeatMapItemResponse toSeatMapItemResponse(SeatStatus ss) {
        return SeatMapItemResponse.builder()
                .seatStatusId(ss.getId())
                .seatId(ss.getSeat().getId())
                .rowLabel(ss.getSeat().getRowLabel())
                .seatNumber(ss.getSeat().getSeatNumber())
                .seatType(ss.getSeat().getSeatType())
                .rowIndex(ss.getSeat().getRowIndex())
                .colIndex(ss.getSeat().getColIndex())
                .priceMultiplier(ss.getSeat().getPriceMultiplier())
                .status(ss.getStatus())
                .build();
    }
}
