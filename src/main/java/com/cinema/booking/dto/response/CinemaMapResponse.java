package com.cinema.booking.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

/**
 * DTO nhẹ dùng cho Leaflet Map — chỉ chứa các field cần thiết để render marker.
 * Tránh trả về toàn bộ CinemaResponse (có rooms, timestamps...) gây lãng phí bandwidth.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CinemaMapResponse {

    UUID    id;
    String  name;
    String  address;
    String  city;
    Double  latitude;
    Double  longitude;
    Boolean isActive;

    /** Khoảng cách tính bằng km — chỉ có giá trị khi gọi /nearest endpoint */
    Double  distanceKm;
}
