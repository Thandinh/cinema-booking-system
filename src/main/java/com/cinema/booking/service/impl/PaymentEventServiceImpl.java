package com.cinema.booking.service.impl;

import com.cinema.booking.dto.request.PaymentEventSearchRequest;
import com.cinema.booking.dto.response.PaymentEventResponse;
import com.cinema.booking.dto.response.PaymentMonitoringSummaryResponse;
import com.cinema.booking.entity.Booking;
import com.cinema.booking.entity.Payment;
import com.cinema.booking.entity.PaymentEvent;
import com.cinema.booking.enums.*;
import com.cinema.booking.repository.PaymentEventRepository;
import com.cinema.booking.repository.projection.PaymentEventSummaryRow;
import com.cinema.booking.service.PaymentEventService;
import com.cinema.booking.service.StaffCinemaScopeService;
import com.cinema.booking.util.DateRange;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PaymentEventServiceImpl implements PaymentEventService {

    PaymentEventRepository paymentEventRepository;
    StaffCinemaScopeService staffCinemaScopeService;

    private static final Set<PaymentEventType> PROVIDER_ERROR_TYPES = EnumSet.of(
            PaymentEventType.PAYMENT_PROVIDER_ERROR,
            PaymentEventType.SEPAY_PAYMENT_NOT_FOUND);
    private static final Set<PaymentEventType> INVALID_SIGNATURE_TYPES = EnumSet.of(
            PaymentEventType.VNPAY_CALLBACK_INVALID_SIGNATURE,
            PaymentEventType.MOMO_CALLBACK_INVALID_SIGNATURE,
            PaymentEventType.SEPAY_WEBHOOK_INVALID_SIGNATURE);
    private static final Set<PaymentEventType> AMOUNT_MISMATCH_TYPES = EnumSet.of(
            PaymentEventType.VNPAY_AMOUNT_MISMATCH,
            PaymentEventType.MOMO_AMOUNT_MISMATCH,
            PaymentEventType.SEPAY_AMOUNT_MISMATCH);

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
        if (staffCinemaScopeService.isStaffButNotAdmin()) {
            List<UUID> cinemaIds = staffCinemaScopeService.getCurrentStaffCinemaIds();
            if (cinemaIds.isEmpty()) {
                return Page.empty(pageable);
            }
            return paymentEventRepository.searchByCinemaIds(
                            safeRequest.getBookingId(),
                            safeRequest.getPaymentId(),
                            safeRequest.getEventType(),
                            safeRequest.getSuccess(),
                            keywordPattern,
                            dateRange.fromSearchBound(),
                            dateRange.toSearchBound(),
                            cinemaIds,
                            pageable)
                    .map(this::toResponse);
        }

        return paymentEventRepository.search(
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

    @Override
    @Transactional(readOnly = true)
    public PaymentMonitoringSummaryResponse getMonitoringSummary(int hours) {
        int safeHours = Math.max(1, Math.min(hours, 24 * 30));
        LocalDateTime toTime = LocalDateTime.now();
        LocalDateTime fromTime = toTime.minusHours(safeHours);

        List<PaymentEventSummaryRow> rows;
        long affectedBookings;
        if (staffCinemaScopeService.isStaffButNotAdmin()) {
            List<UUID> cinemaIds = staffCinemaScopeService.getCurrentStaffCinemaIds();
            if (cinemaIds.isEmpty()) {
                return emptyMonitoringSummary(fromTime, toTime);
            }
            rows = paymentEventRepository.summarizeByCinemaIds(fromTime, toTime, cinemaIds);
            affectedBookings = paymentEventRepository.countDistinctFailedBookingsByCinemaIds(
                    fromTime, toTime, cinemaIds);
        } else {
            rows = paymentEventRepository.summarize(fromTime, toTime);
            affectedBookings = paymentEventRepository.countDistinctFailedBookings(fromTime, toTime);
        }

        long totalEvents = 0;
        long successfulEvents = 0;
        long failedEvents = 0;
        long providerErrors = 0;
        long invalidSignatures = 0;
        long amountMismatches = 0;
        long paymentFailures = 0;
        long expiredPayments = 0;
        Map<PaymentMethod, Long> errorsByMethod = new EnumMap<>(PaymentMethod.class);

        for (PaymentEventSummaryRow row : rows) {
            long total = row.getTotal();
            PaymentEventType eventType = row.getEventType();
            totalEvents += total;
            if (Boolean.TRUE.equals(row.getSuccess())) {
                successfulEvents += total;
            } else if (Boolean.FALSE.equals(row.getSuccess())) {
                failedEvents += total;
                if (row.getMethod() != null) {
                    errorsByMethod.merge(row.getMethod(), total, Long::sum);
                }
            }
            if (PROVIDER_ERROR_TYPES.contains(eventType)) {
                providerErrors += total;
            }
            if (INVALID_SIGNATURE_TYPES.contains(eventType)) {
                invalidSignatures += total;
            }
            if (AMOUNT_MISMATCH_TYPES.contains(eventType)) {
                amountMismatches += total;
            }
            if (eventType == PaymentEventType.PAYMENT_FAILED) {
                paymentFailures += total;
            }
            if (eventType == PaymentEventType.PAYMENT_EXPIRED) {
                expiredPayments += total;
            }
        }

        BigDecimal errorRate = totalEvents == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(failedEvents)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(totalEvents), 2, RoundingMode.HALF_UP);

        return PaymentMonitoringSummaryResponse.builder()
                .fromTime(fromTime)
                .toTime(toTime)
                .totalEvents(totalEvents)
                .successfulEvents(successfulEvents)
                .failedEvents(failedEvents)
                .affectedBookings(affectedBookings)
                .providerErrors(providerErrors)
                .invalidSignatures(invalidSignatures)
                .amountMismatches(amountMismatches)
                .paymentFailures(paymentFailures)
                .expiredPayments(expiredPayments)
                .errorRatePercent(errorRate)
                .healthStatus(resolveHealthStatus(failedEvents, providerErrors, amountMismatches))
                .errorsByMethod(errorsByMethod)
                .build();
    }

    private PaymentMonitoringSummaryResponse emptyMonitoringSummary(
            LocalDateTime fromTime,
            LocalDateTime toTime) {
        return PaymentMonitoringSummaryResponse.builder()
                .fromTime(fromTime)
                .toTime(toTime)
                .errorRatePercent(BigDecimal.ZERO)
                .healthStatus("HEALTHY")
                .errorsByMethod(Map.of())
                .build();
    }

    private String resolveHealthStatus(long failedEvents, long providerErrors, long amountMismatches) {
        if (providerErrors > 0 || amountMismatches > 0) {
            return "CRITICAL";
        }
        return failedEvents > 0 ? "WARNING" : "HEALTHY";
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
