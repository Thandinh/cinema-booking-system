package com.cinema.booking.service.impl;

import com.cinema.booking.dto.request.CinemaCreationRequest;
import com.cinema.booking.dto.request.CinemaUpdateRequest;
import com.cinema.booking.dto.response.CinemaMapResponse;
import com.cinema.booking.dto.response.CinemaResponse;
import com.cinema.booking.configuration.CacheConfig;
import com.cinema.booking.entity.Cinema;
import com.cinema.booking.enums.ErrorCode;
import com.cinema.booking.enums.ShowtimeStatus;
import com.cinema.booking.exception.AppException;
import com.cinema.booking.mapper.CinemaMapper;
import com.cinema.booking.repository.CinemaRepository;
import com.cinema.booking.repository.RoomRepository;
import com.cinema.booking.repository.SeatRepository;
import com.cinema.booking.repository.ShowtimeRepository;
import com.cinema.booking.repository.StaffCinemaRepository;
import com.cinema.booking.service.CinemaService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class CinemaServiceImpl implements CinemaService {

    CinemaRepository cinemaRepository;
    RoomRepository   roomRepository;
    SeatRepository   seatRepository;
    ShowtimeRepository showtimeRepository;
    StaffCinemaRepository staffCinemaRepository;
    CinemaMapper     cinemaMapper;

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheConfig.CINEMAS, allEntries = true),
            @CacheEvict(cacheNames = CacheConfig.CINEMA_MAP, allEntries = true),
            @CacheEvict(cacheNames = CacheConfig.ROOMS_BY_CINEMA, allEntries = true),
            @CacheEvict(cacheNames = CacheConfig.SEATS_BY_ROOM, allEntries = true)
    })
    public CinemaResponse createCinema(CinemaCreationRequest request) {
        if (cinemaRepository.existsByNameAndIsDeletedFalse(request.getName())) {
            throw new AppException(ErrorCode.CINEMA_NAME_EXISTED);
        }
        Cinema saved = cinemaRepository.save(cinemaMapper.toCinema(request));
        log.info("Created cinema: {}", saved.getName());
        return cinemaMapper.toCinemaResponse(saved, false);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheConfig.CINEMAS, allEntries = true),
            @CacheEvict(cacheNames = CacheConfig.CINEMA_MAP, allEntries = true),
            @CacheEvict(cacheNames = CacheConfig.ROOMS_BY_CINEMA, allEntries = true),
            @CacheEvict(cacheNames = CacheConfig.SEATS_BY_ROOM, allEntries = true)
    })
    public CinemaResponse updateCinema(UUID id, CinemaUpdateRequest request) {
        Cinema cinema = findActiveCinemaById(id);

        if (request.getName() != null && !request.getName().equals(cinema.getName())
                && cinemaRepository.existsByNameAndIsDeletedFalse(request.getName())) {
            throw new AppException(ErrorCode.CINEMA_NAME_EXISTED);
        }

        cinemaMapper.updateCinema(cinema, request);
        log.info("Updated cinema id={}", id);
        return cinemaMapper.toCinemaResponse(cinemaRepository.save(cinema), false);
    }

    /**
     * Soft-delete Cinema + cascade soft-delete toàn bộ Rooms và Seats thuộc về rạp.
     * Thực hiện bằng bulk UPDATE (2-3 queries) thay vì load từng entity.
     */
    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheConfig.CINEMAS, allEntries = true),
            @CacheEvict(cacheNames = CacheConfig.CINEMA_MAP, allEntries = true),
            @CacheEvict(cacheNames = CacheConfig.ROOMS_BY_CINEMA, allEntries = true),
            @CacheEvict(cacheNames = CacheConfig.SEATS_BY_ROOM, allEntries = true)
    })
    public void deleteCinema(UUID id) {
        Cinema cinema = findActiveCinemaById(id);

        // Do not delete an operational cinema while active schedules still exist.
        if (showtimeRepository.existsActiveScheduleByCinemaId(
                id,
                List.of(ShowtimeStatus.UPCOMING, ShowtimeStatus.ONGOING),
                LocalDateTime.now())) {
            throw new AppException(ErrorCode.CINEMA_HAS_ACTIVE_SHOWTIMES);
        }

        // 1. Lấy danh sách room IDs để cascade xoá ghế
        List<UUID> roomIds = roomRepository.findActiveRoomIdsByCinemaId(id);

        // 2. Soft-delete tất cả Seats trong các phòng đó (bulk)
        if (!roomIds.isEmpty()) {
            roomIds.forEach(seatRepository::softDeleteByRoomId);
        }

        // 3. Soft-delete tất cả Rooms của rạp (bulk)
        roomRepository.softDeleteByCinemaId(id);

        // 4. Soft-delete Cinema
        cinema.setIsDeleted(true);
        cinemaRepository.save(cinema);
        staffCinemaRepository.deleteByCinemaId(id);

        log.info("Soft-deleted cinema id={} (cascaded {} rooms)", id, roomIds.size());
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheConfig.CINEMAS, key = "'detail:' + #id")
    public CinemaResponse getCinemaById(UUID id) {
        Cinema cinema = cinemaRepository.findActiveByIdWithRooms(id)
                .orElseThrow(() -> new AppException(ErrorCode.CINEMA_NOT_FOUND));
        return cinemaMapper.toCinemaResponse(cinema, true);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = CacheConfig.CINEMAS,
            key = "'list:' + #onlyActive + ':' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort"
    )
    public Page<CinemaResponse> getAllCinemas(Pageable pageable, boolean onlyActive) {
        Page<Cinema> page = onlyActive
                ? cinemaRepository.findAllByIsActiveTrueAndIsDeletedFalse(pageable)
                : cinemaRepository.findAllByIsDeletedFalse(pageable);
        return page.map(c -> cinemaMapper.toCinemaResponse(c, false));
    }

    // =========================================================================
    // MAP DATA
    // =========================================================================

    /**
     * Trả về danh sách rạp đang active và có tọa độ để Leaflet render markers.
     * Không cần auth — public endpoint cho cả khách chưa đăng nhập.
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheConfig.CINEMA_MAP, key = "'all'")
    public List<CinemaMapResponse> getMapData() {
        return cinemaRepository.findAllForMap()
                .stream()
                .map(c -> CinemaMapResponse.builder()
                        .id(c.getId())
                        .name(c.getName())
                        .address(c.getAddress())
                        .city(c.getCity())
                        .latitude(c.getLatitude())
                        .longitude(c.getLongitude())
                        .isActive(c.getIsActive())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Tìm rạp gần nhất bằng Haversine formula (native query PostgreSQL).
     * Kết quả Object[] được map thủ công để tránh thêm entity mới.
     * Clamp limit 1-20 để tránh abuse.
     */
    @Override
    @Transactional(readOnly = true)
    public List<CinemaMapResponse> getNearestCinemas(double lat, double lng, int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 20);
        List<Object[]> rows = cinemaRepository.findNearest(lat, lng, safeLimit);
        return rows.stream()
                .map(row -> CinemaMapResponse.builder()
                        .id(toUUID(row[0]))
                        .name(row[1] != null ? row[1].toString() : null)
                        .address(row[2] != null ? row[2].toString() : null)
                        .city(row[3] != null ? row[3].toString() : null)
                        .latitude(toDouble(row[4]))
                        .longitude(toDouble(row[5]))
                        .isActive(true)
                        .distanceKm(toDouble(row[6]))
                        .build())
                .collect(Collectors.toList());
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private Cinema findActiveCinemaById(UUID id) {
        return cinemaRepository.findActiveById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CINEMA_NOT_FOUND));
    }

    private UUID toUUID(Object val) {
        if (val == null) return null;
        if (val instanceof UUID u) return u;
        return UUID.fromString(val.toString());
    }

    private Double toDouble(Object val) {
        if (val == null) return null;
        if (val instanceof Double d) return d;
        if (val instanceof Number n) return n.doubleValue();
        return Double.parseDouble(val.toString());
    }
}
