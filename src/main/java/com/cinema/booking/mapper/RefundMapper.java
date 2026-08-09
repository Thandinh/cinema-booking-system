package com.cinema.booking.mapper;

import com.cinema.booking.dto.response.RefundResponse;
import com.cinema.booking.entity.Booking;
import com.cinema.booking.entity.Payment;
import com.cinema.booking.entity.Refund;
import com.cinema.booking.entity.Room;
import com.cinema.booking.entity.Showtime;
import com.cinema.booking.entity.User;
import org.springframework.stereotype.Component;

@Component
public class RefundMapper {

    public RefundResponse toResponse(Refund refund) {
        if (refund == null) {
            return null;
        }

        Booking booking = refund.getBooking();
        Payment payment = refund.getPayment();
        User user = booking == null ? null : booking.getUser();
        Showtime showtime = booking == null ? null : booking.getShowtime();
        Room room = showtime == null ? null : showtime.getRoom();

        return RefundResponse.builder()
                .id(refund.getId())
                .bookingId(booking == null ? null : booking.getId())
                .bookingCode(booking == null ? null : "#" + booking.getId().toString().substring(0, 8).toUpperCase())
                .bookingStatus(booking == null ? null : booking.getStatus())
                .paymentId(payment == null ? null : payment.getId())
                .paymentStatus(payment == null ? null : payment.getStatus())
                .method(refund.getMethod())
                .transactionNo(payment == null ? null : payment.getTransactionNo())
                .amount(refund.getAmount())
                .status(refund.getStatus())
                .reason(refund.getReason())
                .providerRefundId(refund.getProviderRefundId())
                .failureReason(refund.getFailureReason())
                .requestedAt(refund.getRequestedAt())
                .processedAt(refund.getProcessedAt())
                .createdAt(refund.getCreatedAt())
                .updatedAt(refund.getUpdatedAt())
                .customerId(user == null ? null : user.getId())
                .customerUsername(user == null ? null : user.getUsername())
                .customerName(buildCustomerName(user))
                .customerEmail(user == null ? null : user.getEmail())
                .movieTitle(showtime == null || showtime.getMovie() == null ? null : showtime.getMovie().getTitle())
                .cinemaId(room == null || room.getCinema() == null ? null : room.getCinema().getId())
                .cinemaName(room == null || room.getCinema() == null ? null : room.getCinema().getName())
                .cinemaCity(room == null || room.getCinema() == null ? null : room.getCinema().getCity())
                .roomName(room == null ? null : room.getName())
                .showtimeStartTime(showtime == null ? null : showtime.getStartTime())
                .build();
    }

    private String buildCustomerName(User user) {
        if (user == null) {
            return null;
        }

        String fullName = ((user.getFirstName() == null ? "" : user.getFirstName()) + " "
                + (user.getLastName() == null ? "" : user.getLastName())).trim();
        return fullName.isBlank() ? user.getUsername() : fullName;
    }
}
