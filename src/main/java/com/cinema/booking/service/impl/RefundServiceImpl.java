package com.cinema.booking.service.impl;

import com.cinema.booking.dto.request.RefundCompleteRequest;
import com.cinema.booking.dto.request.RefundFailRequest;
import com.cinema.booking.dto.request.RefundSearchRequest;
import com.cinema.booking.dto.response.RefundResponse;
import com.cinema.booking.entity.Booking;
import com.cinema.booking.entity.Payment;
import com.cinema.booking.entity.Refund;
import com.cinema.booking.entity.User;
import com.cinema.booking.enums.BookingStatus;
import com.cinema.booking.enums.ErrorCode;
import com.cinema.booking.enums.PaymentEventType;
import com.cinema.booking.enums.PaymentStatus;
import com.cinema.booking.enums.RefundStatus;
import com.cinema.booking.exception.AppException;
import com.cinema.booking.mapper.RefundMapper;
import com.cinema.booking.repository.RefundRepository;
import com.cinema.booking.repository.UserRepository;
import com.cinema.booking.service.PaymentEventService;
import com.cinema.booking.service.RefundService;
import com.cinema.booking.service.StaffCinemaScopeService;
import com.cinema.booking.util.DateRange;
import com.cinema.booking.util.SecurityUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RefundServiceImpl implements RefundService {

    private static final List<RefundStatus> ACTIVE_REFUND_STATUSES = List.of(
            RefundStatus.PENDING,
            RefundStatus.PROCESSING,
            RefundStatus.SUCCESS);

    RefundRepository refundRepository;
    UserRepository userRepository;
    RefundMapper refundMapper;
    StaffCinemaScopeService staffCinemaScopeService;
    PaymentEventService paymentEventService;

    @Override
    @Transactional
    public Refund requestRefund(Payment payment, Booking booking, String reason) {
        return refundRepository.findFirstByPayment_IdAndStatusIn(payment.getId(), ACTIVE_REFUND_STATUSES)
                .orElseGet(() -> refundRepository.save(Refund.builder()
                        .booking(booking)
                        .payment(payment)
                        .amount(payment.getAmount())
                        .method(payment.getMethod())
                        .status(RefundStatus.PENDING)
                        .reason(trimReason(reason))
                        .requestedAt(LocalDateTime.now())
                        .requestedBy(resolveCurrentUser().orElse(null))
                        .build()));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RefundResponse> search(RefundSearchRequest request, Pageable pageable) {
        RefundSearchRequest safeRequest = request == null ? new RefundSearchRequest() : request;
        DateRange dateRange = DateRange.of(safeRequest.getFromDate(), safeRequest.getToDate());
        String keywordPattern = normalizeKeyword(safeRequest.getKeyword());
        String city = normalizeExact(safeRequest.getCity());

        if (staffCinemaScopeService.isStaffButNotAdmin()) {
            List<UUID> cinemaIds = staffCinemaScopeService.getCurrentStaffCinemaIds();
            if (cinemaIds.isEmpty()) {
                return Page.empty(pageable);
            }
            return refundRepository.searchByCinemaIds(
                            safeRequest.getStatus(),
                            safeRequest.getMethod(),
                            keywordPattern,
                            safeRequest.getCinemaId(),
                            city,
                            dateRange.fromSearchBound(),
                            dateRange.toSearchBound(),
                            cinemaIds,
                            pageable)
                    .map(refundMapper::toResponse);
        }

        return refundRepository.search(
                        safeRequest.getStatus(),
                        safeRequest.getMethod(),
                        keywordPattern,
                        safeRequest.getCinemaId(),
                        city,
                        dateRange.fromSearchBound(),
                        dateRange.toSearchBound(),
                        pageable)
                .map(refundMapper::toResponse);
    }

    @Override
    @Transactional
    public RefundResponse markRefunded(UUID refundId, RefundCompleteRequest request) {
        Refund refund = findRefundForUpdate(refundId);
        validateScope(refund);
        ensureRefundCanBeFinalized(refund);

        Booking booking = refund.getBooking();
        Payment payment = refund.getPayment();
        BookingStatus bookingStatusBefore = booking.getStatus();
        PaymentStatus paymentStatusBefore = payment.getStatus();

        refund.setStatus(RefundStatus.SUCCESS);
        refund.setProviderRefundId(trimToNull(request == null ? null : request.getProviderRefundId(), 255));
        refund.setProviderResponse(operatorPayload(request == null ? null : request.getNote()));
        refund.setProcessedAt(LocalDateTime.now());

        payment.setStatus(PaymentStatus.REFUNDED);
        // A late second payment can be refunded while the original booking is
        // still valid. Do not invalidate its tickets by changing SUCCESS.
        if (booking.getStatus() == BookingStatus.REFUND_PENDING) {
            booking.setStatus(BookingStatus.REFUNDED);
        }

        paymentEventService.record(
                payment,
                booking,
                PaymentEventType.REFUND_COMPLETED,
                paymentStatusBefore,
                PaymentStatus.REFUNDED,
                bookingStatusBefore,
                booking.getStatus(),
                true,
                "Refund was marked as completed by operator.",
                refundPayload(refund));

        return refundMapper.toResponse(refund);
    }

    @Override
    @Transactional
    public RefundResponse markRefundFailed(UUID refundId, RefundFailRequest request) {
        Refund refund = findRefundForUpdate(refundId);
        validateScope(refund);
        ensureRefundCanBeFinalized(refund);

        String failureReason = request == null ? null : trimToNull(request.getFailureReason(), 1000);
        if (failureReason == null) {
            throw new AppException(ErrorCode.REFUND_FAILURE_REASON_REQUIRED);
        }

        Booking booking = refund.getBooking();
        Payment payment = refund.getPayment();
        BookingStatus bookingStatusBefore = booking.getStatus();
        PaymentStatus paymentStatusBefore = payment.getStatus();

        refund.setStatus(RefundStatus.FAILED);
        refund.setFailureReason(failureReason);
        refund.setProcessedAt(LocalDateTime.now());
        payment.setStatus(PaymentStatus.REFUND_FAILED);

        paymentEventService.record(
                payment,
                booking,
                PaymentEventType.REFUND_FAILED,
                paymentStatusBefore,
                PaymentStatus.REFUND_FAILED,
                bookingStatusBefore,
                booking.getStatus(),
                false,
                "Refund was marked as failed by operator.",
                refundPayload(refund));

        return refundMapper.toResponse(refund);
    }

    private Optional<User> resolveCurrentUser() {
        try {
            return userRepository.findById(SecurityUtils.getCurrentUserId());
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    private Refund findRefundForUpdate(UUID refundId) {
        return refundRepository.findLockedWithDetailsById(refundId)
                .orElseThrow(() -> new AppException(ErrorCode.REFUND_NOT_FOUND));
    }

    private void validateScope(Refund refund) {
        if (refund.getBooking() == null
                || refund.getBooking().getShowtime() == null
                || refund.getBooking().getShowtime().getRoom() == null
                || refund.getBooking().getShowtime().getRoom().getCinema() == null) {
            return;
        }
        staffCinemaScopeService.validateCurrentStaffCanAccessCinema(
                refund.getBooking().getShowtime().getRoom().getCinema().getId());
    }

    private void ensureRefundCanBeFinalized(Refund refund) {
        if (refund.getStatus() != RefundStatus.PENDING && refund.getStatus() != RefundStatus.PROCESSING) {
            throw new AppException(ErrorCode.REFUND_ALREADY_FINALIZED);
        }
    }

    private String normalizeKeyword(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return "%" + value.trim().toLowerCase(Locale.ROOT) + "%";
    }

    private String normalizeExact(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String trimReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return "Refund requested";
        }
        String trimmed = reason.trim();
        return trimmed.length() <= 500 ? trimmed : trimmed.substring(0, 500);
    }

    private String trimToNull(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }

    private Map<String, Object> operatorPayload(String note) {
        String safeNote = trimToNull(note, 500);
        if (safeNote == null) {
            return null;
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("operatorNote", safeNote);
        return payload;
    }

    private Map<String, Object> refundPayload(Refund refund) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("refundId", refund.getId().toString());
        payload.put("refundStatus", refund.getStatus().name());
        payload.put("amount", refund.getAmount());
        payload.put("method", refund.getMethod().name());
        if (refund.getProviderRefundId() != null) {
            payload.put("providerRefundId", refund.getProviderRefundId());
        }
        if (refund.getFailureReason() != null) {
            payload.put("failureReason", refund.getFailureReason());
        }
        return payload;
    }
}
