package com.cinema.booking.repository;

import com.cinema.booking.entity.Cinema;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CinemaRepository extends JpaRepository<Cinema, UUID> {

    Page<Cinema> findAllByIsDeletedFalse(Pageable pageable);

    Page<Cinema> findAllByIsActiveTrueAndIsDeletedFalse(Pageable pageable);

    @Query("SELECT c FROM Cinema c WHERE c.id = :id AND c.isDeleted = false")
    Optional<Cinema> findActiveById(UUID id);

    boolean existsByNameAndIsDeletedFalse(String name);

    /**
     * Lấy tất cả rạp đang active và có tọa độ hợp lệ — dùng cho Leaflet Map markers.
     * Chỉ trả về rạp có latitude và longitude khác null để tránh null marker.
     */
    @Query("SELECT c FROM Cinema c WHERE c.isDeleted = false AND c.isActive = true " +
           "AND c.latitude IS NOT NULL AND c.longitude IS NOT NULL " +
           "ORDER BY c.name ASC")
    List<Cinema> findAllForMap();

    /**
     * Tìm rạp gần nhất dựa trên tọa độ người dùng (Haversine formula).
     * Trả về Object[]: [cinema_id, name, address, city, latitude, longitude, distance_km]
     * Công thức: R * acos(sin(lat1)*sin(lat2) + cos(lat1)*cos(lat2)*cos(lon2-lon1))
     * với R = 6371 km (bán kính Trái Đất).
     */
    @Query(value = """
            SELECT c.id,
                   c.name,
                   c.address,
                   c.city,
                   c.latitude,
                   c.longitude,
                   (6371 * acos(
                       LEAST(1.0, cos(radians(:lat)) * cos(radians(c.latitude))
                       * cos(radians(c.longitude) - radians(:lng))
                       + sin(radians(:lat)) * sin(radians(c.latitude)))
                   )) AS distance_km
            FROM cinemas c
            WHERE c.is_deleted = false
              AND c.is_active   = true
              AND c.latitude    IS NOT NULL
              AND c.longitude   IS NOT NULL
            ORDER BY distance_km ASC
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> findNearest(
            @Param("lat")   double lat,
            @Param("lng")   double lng,
            @Param("limit") int    limit);
}
