package com.cinema.booking.dto.response;

import com.cinema.booking.enums.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMonitoringSummaryResponse {
    LocalDateTime fromTime;
    LocalDateTime toTime;
    long totalEvents;
    long successfulEvents;
    long failedEvents;
    long affectedBookings;
    long providerErrors;
    long invalidSignatures;
    long amountMismatches;
    long paymentFailures;
    long expiredPayments;
    BigDecimal errorRatePercent;
    String healthStatus;
    Map<PaymentMethod, Long> errorsByMethod;
}
