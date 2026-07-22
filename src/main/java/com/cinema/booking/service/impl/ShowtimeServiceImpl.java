package com.cinema.booking.service.impl;

import com.cinema.booking.dto.request.ShowtimeCreationRequest;
import com.cinema.booking.dto.request.ShowtimeUpdateRequest;
import com.cinema.booking.dto.response.ShowtimeResponse;
import com.cinema.booking.entity.Movie;
import com.cinema.booking.entity.Room;
import com.cinema.booking.entity.Seat;
import com.cinema.booking.entity.SeatStatus;
import com.cinema.booking.entity.Showtime;
import com.cinema.booking.enums.ErrorCode;
import com.cinema.booking.enums.SeatStatusType;
import com.cinema.booking.exception.AppException;
import com.cinema.booking.mapper.ShowtimeMapper;
import com.cinema.booking.repository.MovieRepository;
import com.cinema.booking.repository.RoomRepository;
import com.cinema.booking.repository.SeatRepository;
import com.cinema.booking.repository.SeatStatusRepository;
import com.cinema.booking.repository.ShowtimeRepository;
import com.cinema.booking.service.ShowtimeService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ShowtimeServiceImpl implements ShowtimeService {

    ShowtimeRepository showtimeRepository;
    MovieRepository movieRepository;
    RoomRepository roomRepository;
    SeatRepository seatRepository;
    SeatStatusRepository seatStatusRepository;
    ShowtimeMapper showtimeMapper;

    @NonFinal
    @Value("${showtime.public-days-ahead:7}")
    int publicDaysAhead;

    @NonFinal
    @Value("${showtime.booking-cutoff-minutes:15}")
    int bookingCutoffMinutes;

    @NonFinal
    @Value("${ticket.check-in-early-minutes:30}")
    int checkInEarlyMinutes;

    @NonFinal
    @Value("${ticket.check-in-late-minutes:30}")
    int checkInLateMinutes;

    @Override
    @Transactional
    public ShowtimeResponse createShowtime(ShowtimeCreationRequest request) {
        if (request.getEndTime().isBefore(request.getStartTime()) || request.getEndTime().isEqual(request.getStartTime())) {
            throw new AppException(ErrorCode.SHOWTIME_END_TIME_INVALID);
        }

        Movie movie = movieRepository.findById(request.getMovieId())
                .filter(m -> !m.getIsDeleted())
                .orElseThrow(() -> new AppException(ErrorCode.MOVIE_NOT_FOUND));

        Room room = roomRepository.findActiveById(request.getRoomId())
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));

        // Nâng cấp: Tính thêm 15 phút dọn phòng (Cleaning buffer)
        LocalDateTime startTimeCheck = request.getStartTime().minusMinutes(15);
        LocalDateTime endTimeCheck = request.getEndTime().plusMinutes(15);

        if (showtimeRepository.isTimeOverlapping(room.getId(), startTimeCheck, endTimeCheck)) {
            throw new AppException(ErrorCode.SHOWTIME_TIME_OVERLAPPING);
        }

        Showtime showtime = showtimeMapper.toShowtime(request, movie, room);
        Showtime savedShowtime = showtimeRepository.save(showtime);

        // Tự động clone ghế phòng vào seat_status
        List<Seat> roomSeats = seatRepository.findActiveByRoomId(room.getId());
        if (!roomSeats.isEmpty()) {
            List<SeatStatus> seatStatuses = roomSeats.stream()
                    .map(seat -> SeatStatus.builder()
                            .seat(seat)
                            .showtime(savedShowtime)
                            .status(SeatStatusType.AVAILABLE)
                            .build())
                    .collect(Collectors.toList());
            seatStatusRepository.saveAll(seatStatuses);
        }

        log.info("Created showtime id={} for room={}, auto-generated {} seats", 
                savedShowtime.getId(), room.getName(), roomSeats.size());
        
        return showtimeMapper.toShowtimeResponse(savedShowtime);
    }

    @Override
    @Transactional
    public ShowtimeResponse updateShowtime(UUID id, ShowtimeUpdateRequest request) {
        Showtime showtime = showtimeRepository.findActiveById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SHOWTIME_NOT_FOUND));

        LocalDateTime newStartTime = request.getStartTime() != null ? request.getStartTime() : showtime.getStartTime();
        LocalDateTime newEndTime = request.getEndTime() != null ? request.getEndTime() : showtime.getEndTime();

        if (newEndTime.isBefore(newStartTime) || newEndTime.isEqual(newStartTime)) {
            throw new AppException(ErrorCode.SHOWTIME_END_TIME_INVALID);
        }

        // Kiểm tra overlap (trừ suất hiện tại) nếu có đổi thời gian, bao gồm 15 phút dọn phòng
        if (!newStartTime.equals(showtime.getStartTime()) || !newEndTime.equals(showtime.getEndTime())) {
            LocalDateTime startTimeCheck = newStartTime.minusMinutes(15);
            LocalDateTime endTimeCheck = newEndTime.plusMinutes(15);

            if (showtimeRepository.isTimeOverlappingExclude(showtime.getRoom().getId(), startTimeCheck, endTimeCheck, id)) {
                throw new AppException(ErrorCode.SHOWTIME_TIME_OVERLAPPING);
            }
        }

        showtimeMapper.updateShowtime(showtime, request);
        Showtime saved = showtimeRepository.save(showtime);
        log.info("Updated showtime id={}", id);
        return showtimeMapper.toShowtimeResponse(saved);
    }

    @Override
    @Transactional
    public void deleteShowtime(UUID id) {
        Showtime showtime = showtimeRepository.findActiveById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SHOWTIME_NOT_FOUND));
        
        showtime.setIsDeleted(true);
        showtimeRepository.save(showtime);
        
        // Hard delete các SeatStatus vì nó chứa quá nhiều data và không cần giữ nếu suất chiếu huỷ (hoặc soft delete tuỳ business). 
        // Dùng delete hard để tối ưu DB nếu cần.
        seatStatusRepository.deleteByShowtimeId(id);
        
        log.info("Soft-deleted showtime id={} and removed its seat_status records", id);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ShowtimeResponse> getAllShowtimes(Pageable pageable) {
        return showtimeRepository.findAllActive(pageable)
                .map(showtimeMapper::toShowtimeResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ShowtimeResponse getShowtimeById(UUID id) {
        return showtimeRepository.findActiveById(id)
                .map(showtimeMapper::toShowtimeResponse)
                .orElseThrow(() -> new AppException(ErrorCode.SHOWTIME_NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShowtimeResponse> getShowtimesByMovieId(UUID movieId) {
        ShowtimeSearchWindow window = getPublicShowtimeWindow();
        return showtimeRepository.findBookableByMovieId(movieId, window.fromTime(), window.toTime()).stream()
                .map(showtimeMapper::toShowtimeResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ShowtimeResponse> getShowtimesByCinemaId(UUID cinemaId, Pageable pageable) {
        ShowtimeSearchWindow window = getPublicShowtimeWindow();
        return showtimeRepository.findBookableByCinemaId(cinemaId, window.fromTime(), window.toTime(), pageable)
                .map(showtimeMapper::toShowtimeResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShowtimeResponse> getOpenCheckInShowtimes(UUID cinemaId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime earliestStartTime = now.minusMinutes(Math.max(0, checkInLateMinutes));
        LocalDateTime latestStartTime = now.plusMinutes(Math.max(0, checkInEarlyMinutes));

        return showtimeRepository.findOpenForCheckIn(cinemaId, earliestStartTime, latestStartTime).stream()
                .map(showtimeMapper::toShowtimeResponse)
                .toList();
    }

    private ShowtimeSearchWindow getPublicShowtimeWindow() {
        int safeCutoffMinutes = Math.max(0, bookingCutoffMinutes);
        int safeDaysAhead = Math.max(1, publicDaysAhead);
        LocalDateTime fromTime = LocalDateTime.now().plusMinutes(safeCutoffMinutes);
        LocalDateTime toTime = LocalDate.now().plusDays(safeDaysAhead).atStartOfDay();
        return new ShowtimeSearchWindow(fromTime, toTime);
    }

    private record ShowtimeSearchWindow(LocalDateTime fromTime, LocalDateTime toTime) {
    }
}
