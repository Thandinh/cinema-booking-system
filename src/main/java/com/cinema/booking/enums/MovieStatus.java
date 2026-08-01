package com.cinema.booking.enums;

public enum MovieStatus {
    NOW_SHOWING,  // Đang chiếu
    COMING_SOON,  // Sắp chiếu
    ENDED         // Đã kết thúc — khớp với DB constraint: CHECK (status IN ('NOW_SHOWING', 'COMING_SOON', 'ENDED'))
}
