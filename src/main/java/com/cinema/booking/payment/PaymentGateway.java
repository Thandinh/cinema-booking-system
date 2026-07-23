package com.cinema.booking.payment;

import com.cinema.booking.entity.Booking;
import com.cinema.booking.entity.Payment;
import com.cinema.booking.enums.PaymentMethod;
import jakarta.servlet.http.HttpServletRequest;

public interface PaymentGateway {
    PaymentMethod getMethod();

    String createPaymentUrl(Payment payment, Booking booking, HttpServletRequest request);
}
