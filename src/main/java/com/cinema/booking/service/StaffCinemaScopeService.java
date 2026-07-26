package com.cinema.booking.service;

import com.cinema.booking.enums.ErrorCode;
import com.cinema.booking.exception.AppException;
import com.cinema.booking.repository.StaffCinemaRepository;
import com.cinema.booking.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StaffCinemaScopeService {

    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String ROLE_STAFF = "ROLE_STAFF";

    private final StaffCinemaRepository staffCinemaRepository;

    public boolean isAdmin() {
        return SecurityUtils.hasAuthority(ROLE_ADMIN);
    }

    public boolean isStaffButNotAdmin() {
        return !isAdmin() && SecurityUtils.hasAuthority(ROLE_STAFF);
    }

    @Transactional(readOnly = true)
    public List<UUID> getCurrentStaffCinemaIds() {
        if (!isStaffButNotAdmin()) {
            return List.of();
        }
        return staffCinemaRepository.findCinemaIdsByStaffId(SecurityUtils.getCurrentUserId());
    }

    @Transactional(readOnly = true)
    public void validateCurrentStaffCanAccessCinema(UUID cinemaId) {
        if (cinemaId == null || !isStaffButNotAdmin()) {
            return;
        }
        boolean assigned = staffCinemaRepository.existsByIdStaffIdAndIdCinemaId(
                SecurityUtils.getCurrentUserId(),
                cinemaId);
        if (!assigned) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
    }
}
