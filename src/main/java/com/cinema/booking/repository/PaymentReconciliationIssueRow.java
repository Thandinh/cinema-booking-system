package com.cinema.booking.repository;

import java.time.LocalDateTime;
import java.util.UUID;

public interface PaymentReconciliationIssueRow {
    String getIssueType();
    String getSeverity();
    UUID getBookingId();
    UUID getPaymentId();
    String getTransactionNo();
    String getBookingStatus();
    String getPaymentStatus();
    String getMessage();
    LocalDateTime getCreatedAt();
}
