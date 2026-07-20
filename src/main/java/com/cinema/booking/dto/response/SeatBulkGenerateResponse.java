package com.cinema.booking.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

/**
 * Kết quả trả về sau khi bulk-generate ghế.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SeatBulkGenerateResponse {
    int totalRequested;  // Tổng ghế yêu cầu tạo
    int totalCreated;    // Ghế thực sự được tạo mới
    int totalSkipped;    // Ghế đã tồn tại → bỏ qua
    int totalSeatStatusesCreated;
    List<SeatResponse> createdSeats;
}
