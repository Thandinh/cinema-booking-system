package com.cinema.booking.mapper;

import com.cinema.booking.dto.response.PaymentResponse;
import com.cinema.booking.entity.Booking;
import com.cinema.booking.entity.Payment;
import com.cinema.booking.entity.Showtime;
import com.cinema.booking.entity.User;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {

    public PaymentResponse toPaymentResponse(Payment payment) {
        if (payment == null) return null;

        Booking booking = payment.getBooking();
        User user = booking != null ? booking.getUser() : null;
        Showtime showtime = booking != null ? booking.getShowtime() : null;

        return PaymentResponse.builder()
                .id(payment.getId())
                .bookingId(booking != null ? booking.getId() : null)
                .bookingCode(booking != null ? "#" + booking.getId().toString().substring(0, 8).toUpperCase() : null)
                .bookingStatus(booking != null && booking.getStatus() != null ? booking.getStatus().name() : null)
                .customerId(user != null ? user.getId() : null)
                .customerUsername(user != null ? user.getUsername() : null)
                .customerName(buildCustomerName(user))
                .customerEmail(user != null ? user.getEmail() : null)
                .movieTitle(showtime != null && showtime.getMovie() != null ? showtime.getMovie().getTitle() : null)
                .cinemaName(showtime != null
                        && showtime.getRoom() != null
                        && showtime.getRoom().getCinema() != null
                        ? showtime.getRoom().getCinema().getName()
                        : null)
                .roomName(showtime != null && showtime.getRoom() != null ? showtime.getRoom().getName() : null)
                .showtimeStartTime(showtime != null ? showtime.getStartTime() : null)
                .amount(payment.getAmount())
                .method(payment.getMethod())
                .transactionNo(payment.getTransactionNo())
                .status(payment.getStatus())
                .paymentTime(payment.getPaymentTime())
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
