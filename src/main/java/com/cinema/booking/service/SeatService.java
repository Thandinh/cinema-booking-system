package com.cinema.booking.service;

import com.cinema.booking.dto.request.SeatBulkGenerateRequest;
import com.cinema.booking.dto.request.SeatCreationRequest;
import com.cinema.booking.dto.request.SeatUpdateRequest;
import com.cinema.booking.dto.response.SeatBulkGenerateResponse;
import com.cinema.booking.dto.response.SeatResponse;

import java.util.List;
import java.util.UUID;

public interface SeatService {

    /** Tạo 1 ghế đơn lẻ */
    SeatResponse createSeat(SeatCreationRequest request);

    /**
     * Tự động sinh sơ đồ ghế hàng loạt cho một phòng.
     * rowIndex và colIndex được tính tự động theo vị trí trong rowLabels và seatsPerRow.
     */
    SeatBulkGenerateResponse bulkGenerateSeats(SeatBulkGenerateRequest request);

    /** Cập nhật loại ghế / hệ số giá / vị trí grid */
    SeatResponse updateSeat(UUID id, SeatUpdateRequest request);

    /** Xoá mềm ghế */
    void deleteSeat(UUID id);

    /** Lấy chi tiết 1 ghế */
    SeatResponse getSeatById(UUID id);

    /** Lấy toàn bộ ghế của 1 phòng chiếu */
    List<SeatResponse> getSeatsByRoomId(UUID roomId);
}
