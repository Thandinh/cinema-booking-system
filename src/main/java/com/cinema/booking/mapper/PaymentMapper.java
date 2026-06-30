package com.cinema.booking.mapper;

import com.cinema.booking.dto.response.PaymentResponse;
import com.cinema.booking.entity.Payment;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {

    public PaymentResponse toPaymentResponse(Payment payment) {
        if (payment == null) return null;

        return PaymentResponse.builder()
                .id(payment.getId())
                .bookingId(payment.getBooking() != null ? payment.getBooking().getId() : null)
                .amount(payment.getAmount())
                .method(payment.getMethod())
                .transactionNo(payment.getTransactionNo())
                .status(payment.getStatus())
                .paymentTime(payment.getPaymentTime())
                .build();
    }
}
