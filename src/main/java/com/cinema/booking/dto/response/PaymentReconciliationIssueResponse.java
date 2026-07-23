package com.cinema.booking.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentReconciliationIssueResponse {
    String issueType;
    String severity;
    UUID bookingId;
    UUID paymentId;
    String transactionNo;
    String bookingStatus;
    String paymentStatus;
    String message;
    LocalDateTime createdAt;
}
