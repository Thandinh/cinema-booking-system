package com.cinema.booking.dto.response;

import com.cinema.booking.enums.TicketStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TicketResponse {
    UUID id;
    String qrCode;
    String qrImage;
    TicketStatus status;
    LocalDateTime checkInTime;
    UUID checkedInById;
    String checkedInByUsername;
    String checkedInByName;
    Boolean alreadyCheckedIn;

    // Thông tin vé để hiển thị
    String movieTitle;
    String cinemaName;
    String roomName;
    LocalDateTime startTime;
    String rowLabel;
    Integer seatNumber;
    String seatType;
}
