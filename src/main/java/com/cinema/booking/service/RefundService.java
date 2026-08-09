package com.cinema.booking.service;

import com.cinema.booking.dto.request.RefundCompleteRequest;
import com.cinema.booking.dto.request.RefundFailRequest;
import com.cinema.booking.dto.request.RefundSearchRequest;
import com.cinema.booking.dto.response.RefundResponse;
import com.cinema.booking.entity.Booking;
import com.cinema.booking.entity.Payment;
import com.cinema.booking.entity.Refund;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface RefundService {

    Refund requestRefund(Payment payment, Booking booking, String reason);

    Page<RefundResponse> search(RefundSearchRequest request, Pageable pageable);

    RefundResponse markRefunded(UUID refundId, RefundCompleteRequest request);

    RefundResponse markRefundFailed(UUID refundId, RefundFailRequest request);
}
