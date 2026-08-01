package com.cinema.booking.service;

import com.cinema.booking.dto.request.CinemaCreationRequest;
import com.cinema.booking.dto.request.CinemaUpdateRequest;
import com.cinema.booking.dto.response.CinemaMapResponse;
import com.cinema.booking.dto.response.CinemaResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface CinemaService {
    CinemaResponse createCinema(CinemaCreationRequest request);
    CinemaResponse updateCinema(UUID id, CinemaUpdateRequest request);
    void deleteCinema(UUID id);
    CinemaResponse getCinemaById(UUID id);
    Page<CinemaResponse> getAllCinemas(Pageable pageable, boolean onlyActive);

    /**
     * Trả về danh sách tất cả rạp có tọa độ — dùng để render Leaflet markers.
     * Endpoint public (không cần auth) để frontend có thể gọi mà không cần login.
     */
    List<CinemaMapResponse> getMapData();

    /**
     * Tìm tối đa {@code limit} rạp gần nhất với tọa độ (lat, lng) của người dùng.
     * Khoảng cách tính theo Haversine formula, đơn vị km.
     *
     * @param lat   vĩ độ người dùng
     * @param lng   kinh độ người dùng
     * @param limit số rạp tối đa muốn lấy (mặc định 5)
     */
    List<CinemaMapResponse> getNearestCinemas(double lat, double lng, int limit);
}

