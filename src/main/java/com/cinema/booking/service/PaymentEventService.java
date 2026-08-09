package com.cinema.booking.service;

import com.cinema.booking.dto.request.PaymentEventSearchRequest;
import com.cinema.booking.dto.response.PaymentEventResponse;
import com.cinema.booking.dto.response.PaymentMonitoringSummaryResponse;
import com.cinema.booking.entity.Booking;
import com.cinema.booking.entity.Payment;
import com.cinema.booking.enums.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.UUID;

public interface PaymentEventService {

    void record(
            Payment payment,
            Booking booking,
            PaymentEventType eventType,
            PaymentStatus paymentStatusBefore,
            PaymentStatus paymentStatusAfter,
            BookingStatus bookingStatusBefore,
            BookingStatus bookingStatusAfter,
            Boolean success,
            String message,
            Map<String, Object> payload);

    void recordDetached(
            UUID paymentId,
            UUID bookingId,
            PaymentMethod method,
            String transactionNo,
            PaymentEventType eventType,
            PaymentStatus paymentStatusBefore,
            PaymentStatus paymentStatusAfter,
            BookingStatus bookingStatusBefore,
            BookingStatus bookingStatusAfter,
            Boolean success,
            String message,
            Map<String, Object> payload);

    Page<PaymentEventResponse> search(PaymentEventSearchRequest request, Pageable pageable);

    PaymentMonitoringSummaryResponse getMonitoringSummary(int hours);
}
