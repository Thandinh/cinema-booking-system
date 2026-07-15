package com.cinema.booking.mapper;

import com.cinema.booking.dto.response.BookingDetailResponse;
import com.cinema.booking.dto.response.BookingResponse;
import com.cinema.booking.dto.response.SeatMapItemResponse;
import com.cinema.booking.entity.Booking;
import com.cinema.booking.entity.BookingDetail;
import com.cinema.booking.entity.SeatStatus;
import com.cinema.booking.service.QrCodeImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class BookingMapper {

    private final QrCodeImageService qrCodeImageService;

    public BookingDetailResponse toBookingDetailResponse(BookingDetail detail) {
        String ticketQrCode = detail.getTicket() != null ? detail.getTicket().getQrCode() : null;
        String ticketStatus = detail.getTicket() != null ? detail.getTicket().getStatus().name() : null;

        return BookingDetailResponse.builder()
                .id(detail.getId())
                .seatId(detail.getSeat().getId())
                .rowLabel(detail.getSeat().getRowLabel())
                .seatNumber(detail.getSeat().getSeatNumber())
                .seatType(detail.getSeat().getSeatType())
                .priceAtBooking(detail.getPriceAtBooking())
                .ticketId(detail.getTicket() != null ? detail.getTicket().getId() : null)
                .ticketStatus(ticketStatus)
                .ticketCheckInTime(detail.getTicket() != null ? detail.getTicket().getCheckInTime() : null)
                // QR code chỉ có khi booking thành công và ticket đã được tạo
                .ticketQrCode(ticketQrCode)
                .ticketQrImage(ticketQrCode != null ? qrCodeImageService.toPngDataUri(ticketQrCode, 360) : null)
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
                .cinemaAddress(booking.getShowtime().getRoom().getCinema().getAddress())
                .cinemaCity(booking.getShowtime().getRoom().getCinema().getCity())
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
        java.math.BigDecimal basePrice = ss.getShowtime().getBasePrice();
        java.math.BigDecimal multiplier = ss.getSeat().getPriceMultiplier() != null
                ? ss.getSeat().getPriceMultiplier()
                : java.math.BigDecimal.ONE;
        java.math.BigDecimal price = basePrice.multiply(multiplier);

        return SeatMapItemResponse.builder()
                .seatStatusId(ss.getId())
                .seatId(ss.getSeat().getId())
                .rowLabel(ss.getSeat().getRowLabel())
                .seatNumber(ss.getSeat().getSeatNumber())
                .seatType(ss.getSeat().getSeatType())
                .rowIndex(ss.getSeat().getRowIndex())
                .colIndex(ss.getSeat().getColIndex())
                .priceMultiplier(multiplier)
                .price(price)
                .status(ss.getStatus())
                .build();
    }
}
