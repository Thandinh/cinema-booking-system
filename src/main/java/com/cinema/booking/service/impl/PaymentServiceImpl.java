package com.cinema.booking.service.impl;

import com.cinema.booking.dto.response.PaymentResponse;
import com.cinema.booking.entity.Booking;
import com.cinema.booking.entity.Payment;
import com.cinema.booking.enums.BookingStatus;
import com.cinema.booking.enums.ErrorCode;
import com.cinema.booking.enums.PaymentMethod;
import com.cinema.booking.enums.PaymentStatus;
import com.cinema.booking.exception.AppException;
import com.cinema.booking.repository.BookingRepository;
import com.cinema.booking.repository.PaymentRepository;
import com.cinema.booking.service.PaymentService;
import com.cinema.booking.util.SecurityUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    PaymentRepository paymentRepository;
    BookingRepository bookingRepository;

    @Override
    @Transactional
    public String initiatePayment(UUID bookingId, PaymentMethod method, BigDecimal amount) {
        UUID userId = SecurityUtils.getCurrentUserId();

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        if (!booking.getUser().getId().equals(userId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new AppException(ErrorCode.BOOKING_ALREADY_PROCESSED);
        }

        // Tạo record Payment PENDING
        String txnNo = "TXN" + System.currentTimeMillis();
        Payment payment = Payment.builder()
                .booking(booking)
                .amount(amount)
                .method(method)
                .transactionNo(txnNo)
                .status(PaymentStatus.PENDING)
                .build();
        
        paymentRepository.save(payment);
        log.info("Initiated payment {} for booking {}", txnNo, bookingId);

        // TODO: Thực tế sẽ gọi SDK của VNPay/MoMo ở đây để lấy URL
        // Tạm thời giả lập trả về một URL mock có chứa token của booking
        return "https://mock-payment-gateway.com/pay?txn=" + txnNo + "&token=" + booking.getSecureToken();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentResponse> getMyPayments(Pageable pageable) {
        UUID userId = SecurityUtils.getCurrentUserId();
        return paymentRepository.findByUserId(userId, pageable)
                .map(this::toPaymentResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentResponse> getAllPayments(Pageable pageable) {
        return paymentRepository.findAllWithDetails(pageable)
                .map(this::toPaymentResponse);
    }

    private PaymentResponse toPaymentResponse(Payment payment) {
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
