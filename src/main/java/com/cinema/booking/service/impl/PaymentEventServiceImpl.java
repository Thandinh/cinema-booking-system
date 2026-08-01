package com.cinema.booking.service.impl;

import com.cinema.booking.dto.request.PaymentEventSearchRequest;
import com.cinema.booking.dto.response.PaymentEventResponse;
import com.cinema.booking.entity.Booking;
import com.cinema.booking.entity.Payment;
import com.cinema.booking.entity.PaymentEvent;
import com.cinema.booking.enums.*;
import com.cinema.booking.repository.PaymentEventRepository;
import com.cinema.booking.service.PaymentEventService;
import com.cinema.booking.util.DateRange;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PaymentEventServiceImpl implements PaymentEventService {

    PaymentEventRepository paymentEventRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(
            Payment payment,
            Booking booking,
            PaymentEventType eventType,
            PaymentStatus paymentStatusBefore,
            PaymentStatus paymentStatusAfter,
            BookingStatus bookingStatusBefore,
            BookingStatus bookingStatusAfter,
            Boolean success,
            String message,
            Map<String, Object> payload) {
        recordDetached(
                payment != null ? payment.getId() : null,
                booking != null ? booking.getId() : null,
                payment != null ? payment.getMethod() : null,
                payment != null ? payment.getTransactionNo() : null,
                eventType,
                paymentStatusBefore,
                paymentStatusAfter,
                bookingStatusBefore,
                bookingStatusAfter,
                success,
                message,
                payload);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordDetached(
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
            Map<String, Object> payload) {
        PaymentEvent event = PaymentEvent.builder()
                .paymentId(paymentId)
                .bookingId(bookingId)
                .method(method)
                .transactionNo(transactionNo)
                .eventType(eventType)
                .paymentStatusBefore(paymentStatusBefore)
                .paymentStatusAfter(paymentStatusAfter)
                .bookingStatusBefore(bookingStatusBefore)
                .bookingStatusAfter(bookingStatusAfter)
                .success(success)
                .message(message)
                .payload(payload == null ? null : new LinkedHashMap<>(payload))
                .build();
        paymentEventRepository.save(event);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentEventResponse> search(PaymentEventSearchRequest request, Pageable pageable) {
        PaymentEventSearchRequest safeRequest = request == null ? new PaymentEventSearchRequest() : request;
        DateRange dateRange = DateRange.of(safeRequest.getFromDate(), safeRequest.getToDate());
        String keywordPattern = safeRequest.getKeyword() == null || safeRequest.getKeyword().isBlank()
                ? null
                : "%" + safeRequest.getKeyword().trim().toLowerCase(Locale.ROOT) + "%";
        return paymentEventRepository
                .search(
                        safeRequest.getBookingId(),
                        safeRequest.getPaymentId(),
                         safeRequest.getEventType(),
                         safeRequest.getSuccess(),
                         keywordPattern,
                        dateRange.fromSearchBound(),
                        dateRange.toSearchBound(),
                         pageable)
                 .map(this::toResponse);
     }

    private PaymentEventResponse toResponse(PaymentEvent event) {
        return PaymentEventResponse.builder()
                .id(event.getId())
                .paymentId(event.getPaymentId())
                .bookingId(event.getBookingId())
                .method(event.getMethod())
                .transactionNo(event.getTransactionNo())
                .eventType(event.getEventType())
                .paymentStatusBefore(event.getPaymentStatusBefore())
                .paymentStatusAfter(event.getPaymentStatusAfter())
                .bookingStatusBefore(event.getBookingStatusBefore())
                .bookingStatusAfter(event.getBookingStatusAfter())
                .success(event.getSuccess())
                .message(event.getMessage())
                .payload(event.getPayload())
                .createdAt(event.getCreatedAt())
                .build();
    }
}
