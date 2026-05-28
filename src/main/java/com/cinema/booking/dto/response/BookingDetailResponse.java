package com.cinema.booking.dto.response;

import com.cinema.booking.enums.SeatType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookingDetailResponse {
    UUID id;
    UUID seatId;
    String rowLabel;
    Integer seatNumber;
    SeatType seatType;
    BigDecimal priceAtBooking;
    String ticketQrCode;   // null nếu booking chưa thành công
}
