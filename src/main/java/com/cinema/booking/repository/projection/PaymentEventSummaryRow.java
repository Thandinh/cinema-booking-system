package com.cinema.booking.repository.projection;

import com.cinema.booking.enums.PaymentEventType;
import com.cinema.booking.enums.PaymentMethod;

public interface PaymentEventSummaryRow {
    PaymentEventType getEventType();
    PaymentMethod getMethod();
    Boolean getSuccess();
    long getTotal();
}
