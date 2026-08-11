package com.cinema.booking.enums;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum ErrorCode {
    // ── Generic ──────────────────────────────────────────────────────────────
    UNCATEGORIZED_EXCEPTION(9999, "He thong dang gap su co. Vui long thu lai sau.", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_KEY            (1001, "Thong tin khong hop le",                         HttpStatus.BAD_REQUEST),

    // ── User ─────────────────────────────────────────────────────────────────
    USER_EXISTED           (1002, "Ten dang nhap da ton tai",             HttpStatus.CONFLICT),
    EMAIL_EXISTED          (1003, "Email da ton tai",                     HttpStatus.CONFLICT),
    ROLE_NOT_FOUND         (1004, "Khong tim thay vai tro",               HttpStatus.NOT_FOUND),
    USER_NOT_FOUND         (1005, "Khong tim thay tai khoan",             HttpStatus.NOT_FOUND),
    UNAUTHENTICATED        (1006, "Phien dang nhap khong hop le hoac da het han", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED           (1007, "Bạn không có quyền thực hiện thao tác này", HttpStatus.FORBIDDEN),
    USER_NOT_ACTIVE        (1008, "Tai khoan da bi khoa hoac ngung hoat dong", HttpStatus.FORBIDDEN),
    CANNOT_BLOCK_SELF      (1009, "Khong the khoa tai khoan cua chinh minh", HttpStatus.BAD_REQUEST),
    CANNOT_DELETE_SELF     (1040, "Khong the xoa tai khoan cua chinh minh", HttpStatus.BAD_REQUEST),
    CANNOT_CHANGE_OWN_ADMIN_ROLE(1041, "Khong the go vai tro ADMIN cua chinh minh", HttpStatus.BAD_REQUEST),

    // ── Validation keys — matched by GlobalExceptionHandler via Enum.valueOf() ─
    USERNAME_INVALID       (1010, "Ten dang nhap phai co tu {min} den 50 ky tu", HttpStatus.BAD_REQUEST),
    PASSWORD_INVALID       (1011, "Mat khau phai co 8-72 ky tu, gom chu hoa, chu thuong, so, ky tu dac biet va khong co khoang trang", HttpStatus.BAD_REQUEST),
    EMAIL_INVALID          (1012, "Email khong dung dinh dang",                      HttpStatus.BAD_REQUEST),
    PHONE_INVALID          (1013, "So dien thoai Viet Nam khong hop le",             HttpStatus.BAD_REQUEST),
    DOB_INVALID            (1014, "Ngay sinh phai nam trong qua khu",                HttpStatus.BAD_REQUEST),
    USERNAME_REQUIRED      (1015, "Vui long nhap ten dang nhap",                     HttpStatus.BAD_REQUEST),
    PASSWORD_REQUIRED      (1016, "Vui long nhap mat khau",                          HttpStatus.BAD_REQUEST),
    FIRSTNAME_REQUIRED     (1017, "Vui long nhap ten",                               HttpStatus.BAD_REQUEST),
    LASTNAME_REQUIRED      (1018, "Vui long nhap ho",                                HttpStatus.BAD_REQUEST),
    EMAIL_REQUIRED         (1019, "Vui long nhap email",                             HttpStatus.BAD_REQUEST),
    EMAIL_NOT_VERIFIED     (1020, "Vui long xac thuc email truoc khi dang nhap",     HttpStatus.FORBIDDEN),
    EMAIL_VERIFICATION_INVALID(1021, "Lien ket xac thuc email khong hop le hoac da het han", HttpStatus.BAD_REQUEST),
    CURRENT_PASSWORD_INVALID(1022, "Mat khau hien tai khong dung",                   HttpStatus.BAD_REQUEST),
    PASSWORD_CONFIRM_MISMATCH(1023, "Xac nhan mat khau khong khop",                  HttpStatus.BAD_REQUEST),
    PASSWORD_RESET_INVALID(1024, "Lien ket dat lai mat khau khong hop le hoac da het han", HttpStatus.BAD_REQUEST),
    PARAMETER_INVALID      (1025, "Tham so yeu cau khong hop le",                   HttpStatus.BAD_REQUEST),
    PARAMETER_REQUIRED     (1026, "Thieu tham so bat buoc",                          HttpStatus.BAD_REQUEST),
    REQUEST_BODY_INVALID   (1027, "Du lieu gui len khong hop le",                    HttpStatus.BAD_REQUEST),
    DATA_INTEGRITY_VIOLATION(1028, "Du lieu xung dot voi ban ghi hien co",           HttpStatus.CONFLICT),
    ROLE_NAME_REQUIRED     (1029, "Vui long nhap ten vai tro",                       HttpStatus.BAD_REQUEST),
    ROLE_NAME_INVALID      (1030, "Ten vai tro phai co tu {min} den 50 ky tu",      HttpStatus.BAD_REQUEST),
    PERMISSION_NAME_REQUIRED(1031, "Vui long nhap ten quyen",                        HttpStatus.BAD_REQUEST),
    PERMISSION_NAME_INVALID(1032, "Ten quyen phai co tu {min} den 100 ky tu",       HttpStatus.BAD_REQUEST),
    GOOGLE_ID_TOKEN_REQUIRED(1033, "Thieu Google ID token",                          HttpStatus.BAD_REQUEST),
    AVATAR_URL_INVALID     (1034, "URL anh dai dien toi da 500 ky tu",               HttpStatus.BAD_REQUEST),
    AUTH_RATE_LIMITED      (1035, "Ban thao tac qua nhanh. Vui long thu lai sau",    HttpStatus.TOO_MANY_REQUESTS),
    RESOURCE_NOT_FOUND     (1036, "Khong tim thay tai nguyen",                       HttpStatus.NOT_FOUND),
    METHOD_NOT_ALLOWED     (1037, "Phuong thuc HTTP khong duoc ho tro",              HttpStatus.METHOD_NOT_ALLOWED),
    MEDIA_TYPE_NOT_SUPPORTED(1038, "Kieu du lieu gui len khong duoc ho tro",         HttpStatus.UNSUPPORTED_MEDIA_TYPE),
    CONCURRENT_UPDATE_CONFLICT(1039, "Du lieu vua duoc cap nhat boi yeu cau khac. Vui long tai lai va thu lai", HttpStatus.CONFLICT),
    DATE_RANGE_INVALID     (1042, "Ngay bat dau phai truoc hoac bang ngay ket thuc", HttpStatus.BAD_REQUEST),

    // ── Movie ────────────────────────────────────────────────────────────────
    MOVIE_NOT_FOUND        (2001, "Khong tim thay phim",                  HttpStatus.NOT_FOUND),
    MOVIE_TITLE_EXISTED    (2002, "Ten phim da ton tai",                  HttpStatus.CONFLICT),
    MOVIE_HAS_ACTIVE_SHOWTIMES(2003, "Phim con suat chieu dang hoat dong, khong the xoa", HttpStatus.CONFLICT),
    MOVIE_TITLE_REQUIRED   (2010, "Vui long nhap ten phim",               HttpStatus.BAD_REQUEST),
    MOVIE_DURATION_REQUIRED(2011, "Vui long nhap thoi luong phim",        HttpStatus.BAD_REQUEST),
    MOVIE_DURATION_INVALID (2012, "Thoi luong phim phai lon hon 0",       HttpStatus.BAD_REQUEST),
    MOVIE_RELEASE_DATE_REQUIRED(2013, "Vui long nhap ngay khoi chieu",    HttpStatus.BAD_REQUEST),
    MOVIE_STATUS_REQUIRED  (2014, "Vui long chon trang thai phim",        HttpStatus.BAD_REQUEST),

    // ── Cinema & Room ────────────────────────────────────────────────────────
    CINEMA_NOT_FOUND       (3001, "Khong tim thay rap",                   HttpStatus.NOT_FOUND),
    CINEMA_NAME_EXISTED    (3002, "Ten rap da ton tai",                   HttpStatus.CONFLICT),
    ROOM_NOT_FOUND         (3003, "Khong tim thay phong chieu",           HttpStatus.NOT_FOUND),
    ROOM_NAME_EXISTED      (3004, "Ten phong da ton tai trong rap nay",   HttpStatus.CONFLICT),
    CINEMA_HAS_ACTIVE_SHOWTIMES(3005, "Rap con suat chieu dang hoat dong, khong the xoa", HttpStatus.CONFLICT),
    ROOM_HAS_ACTIVE_SHOWTIMES(3006, "Phong con suat chieu dang hoat dong, khong the xoa", HttpStatus.CONFLICT),
    CINEMA_NAME_REQUIRED   (3010, "Vui long nhap ten rap",                HttpStatus.BAD_REQUEST),
    ROOM_NAME_REQUIRED     (3011, "Vui long nhap ten phong",              HttpStatus.BAD_REQUEST),
    CINEMA_ID_REQUIRED     (3012, "Vui long chon rap",                    HttpStatus.BAD_REQUEST),
    ROOM_ID_REQUIRED       (3013, "Vui long chon phong chieu",            HttpStatus.BAD_REQUEST),

    // ── Seat ────────────────────────────────────────────────────────────────
    SEAT_NOT_FOUND         (4001, "Khong tim thay ghe",                   HttpStatus.NOT_FOUND),
    SEAT_ALREADY_EXISTS    (4002, "Ghe da ton tai trong phong nay",       HttpStatus.CONFLICT),
    SEAT_IN_USE            (4003, "Ghe dang duoc giu hoac da dat",        HttpStatus.CONFLICT),
    SEAT_ROW_REQUIRED      (4010, "Vui long nhap hang ghe",               HttpStatus.BAD_REQUEST),
    SEAT_NUMBER_REQUIRED   (4011, "Vui long nhap so ghe",                 HttpStatus.BAD_REQUEST),
    SEAT_NUMBER_INVALID    (4012, "So ghe phai lon hon hoac bang 1",      HttpStatus.BAD_REQUEST),
    SEAT_MULTIPLIER_INVALID(4013, "He so gia ghe phai lon hon hoac bang 0", HttpStatus.BAD_REQUEST),
    SEAT_ROW_SIZE_INVALID  (4014, "Hang ghe phai co tu 1 den 26 vi tri",  HttpStatus.BAD_REQUEST),

    // ── Showtime ────────────────────────────────────────────────────────────
    SHOWTIME_NOT_FOUND         (5001, "Khong tim thay suat chieu",             HttpStatus.NOT_FOUND),
    SHOWTIME_TIME_OVERLAPPING  (5002, "Thoi gian bi trung voi suat chieu khac trong phong", HttpStatus.CONFLICT),
    SHOWTIME_END_TIME_INVALID  (5003, "Gio ket thuc phai sau gio bat dau",     HttpStatus.BAD_REQUEST),
    SHOWTIME_HAS_ACTIVE_BOOKINGS(5004, "Suat chieu co don dang xu ly, khong the xoa", HttpStatus.CONFLICT),
    SHOWTIME_HAS_USED_TICKETS  (5005, "Suat chieu da co ve duoc soat, can xu ly su co thu cong", HttpStatus.CONFLICT),
    SHOWTIME_NOT_CANCELLABLE   (5006, "Chi co the huy suat chieu sap chieu hoac dang chieu", HttpStatus.CONFLICT),
    MOVIE_ID_REQUIRED          (5010, "Vui long chon phim",                     HttpStatus.BAD_REQUEST),
    START_TIME_REQUIRED        (5011, "Vui long chon gio bat dau",              HttpStatus.BAD_REQUEST),
    END_TIME_REQUIRED          (5012, "Vui long chon gio ket thuc",             HttpStatus.BAD_REQUEST),
    SHOWTIME_CANCEL_REASON_REQUIRED(5017, "Vui long nhap ly do huy suat",       HttpStatus.BAD_REQUEST),
    SHOWTIME_CANCEL_REASON_INVALID(5018, "Ly do huy suat toi da 500 ky tu",     HttpStatus.BAD_REQUEST),

    // ── Promotion ────────────────────────────────────────────────────────
    PROMOTION_CODE_EXISTS      (6001, "Ma giam gia da ton tai",                HttpStatus.CONFLICT),
    PROMOTION_NOT_FOUND        (6002, "Khong tim thay ma giam gia",            HttpStatus.NOT_FOUND),
    PROMOTION_END_DATE_INVALID (6011, "Ngay ket thuc phai sau ngay bat dau",   HttpStatus.BAD_REQUEST),
    DISCOUNT_VALUE_INVALID     (6014, "Gia tri giam khong hop le",             HttpStatus.BAD_REQUEST),
    MIN_ORDER_INVALID          (6015, "Gia tri don toi thieu phai lon hon hoac bang 0", HttpStatus.BAD_REQUEST),
    START_DATE_REQUIRED        (6016, "Vui long chon ngay bat dau",             HttpStatus.BAD_REQUEST),
    END_DATE_REQUIRED          (6017, "Vui long chon ngay ket thuc",            HttpStatus.BAD_REQUEST),
    PROMOTION_NOT_ACTIVE       (6018, "Ma giam gia chua duoc kich hoat",        HttpStatus.BAD_REQUEST),
    PROMOTION_EXPIRED          (6019, "Ma giam gia da het han",                 HttpStatus.BAD_REQUEST),
    PROMOTION_LIMIT_REACHED    (6020, "Ma giam gia da het luot su dung",        HttpStatus.BAD_REQUEST),
    PROMOTION_MIN_ORDER_NOT_MET(6021, "Gia tri don hang chua dat muc toi thieu", HttpStatus.BAD_REQUEST),
    DISCOUNT_TYPE_REQUIRED     (6022, "Vui long chon loai giam gia",            HttpStatus.BAD_REQUEST),
    DISCOUNT_VALUE_REQUIRED    (6023, "Vui long nhap gia tri giam",             HttpStatus.BAD_REQUEST),
    MAX_DISCOUNT_INVALID       (6024, "Muc giam toi da phai lon hon hoac bang 0", HttpStatus.BAD_REQUEST),
    END_DATE_FUTURE            (6025, "Ngay ket thuc phai nam trong tuong lai", HttpStatus.BAD_REQUEST),
    PROMOTION_CODE_REQUIRED    (6026, "Vui long nhap ma giam gia",              HttpStatus.BAD_REQUEST),

    // ── Seat Availability ────────────────────────────────────────────────
    SEAT_NOT_AVAILABLE         (4020, "Ghế vừa được người khác giữ hoặc đã được đặt. Vui lòng chọn ghế khác.", HttpStatus.CONFLICT),
    SEAT_NOT_HELD              (4021, "Ghe chua duoc giu",                       HttpStatus.CONFLICT),
    SEAT_HELD_BY_ANOTHER       (4022, "Ghe dang duoc nguoi dung khac giu",      HttpStatus.CONFLICT),
    SEAT_HOLD_EXPIRED          (4023, "Thoi gian giu ghe da het. Vui long chon lai", HttpStatus.CONFLICT),
    SEAT_IDS_REQUIRED          (4024, "Vui long chon it nhat mot ghe",          HttpStatus.BAD_REQUEST),
    SEAT_IDS_SIZE_INVALID      (4025, "Moi don toi da 10 ghe",                  HttpStatus.BAD_REQUEST),
    SEAT_HOLD_RATE_LIMITED     (4026, "Bạn thao tác giữ ghế quá nhanh. Vui lòng đợi một chút rồi thử lại.", HttpStatus.TOO_MANY_REQUESTS),
    SHOWTIME_NOT_BOOKABLE      (5020, "Suat chieu hien khong mo ban",           HttpStatus.BAD_REQUEST),
    SHOWTIME_ID_REQUIRED       (5021, "Vui long chon suat chieu",               HttpStatus.BAD_REQUEST),

    // ── Booking ───────────────────────────────────────────────────────────
    BOOKING_NOT_FOUND          (7001, "Khong tim thay don dat ve",              HttpStatus.NOT_FOUND),
    BOOKING_ALREADY_PROCESSED  (7002, "Don dat ve da duoc xu ly",                HttpStatus.CONFLICT),
    BOOKING_CANNOT_CANCEL      (7003, "Chi co the huy don dang cho thanh toan", HttpStatus.CONFLICT),
    BOOKING_EXPIRED            (7004, "Thoi gian thanh toan da het",            HttpStatus.CONFLICT),
    BOOKING_PENDING_EXISTS     (7005, "Bạn đang có một đơn chờ thanh toán cho suất chiếu này. Hãy hoàn tất hoặc hủy đơn hiện tại trước khi chọn ghế khác.", HttpStatus.CONFLICT),

    // ── Ticket ───────────────────────────────────────────────────────────
    TICKET_NOT_FOUND           (8001, "Khong tim thay ve",                      HttpStatus.NOT_FOUND),
    TICKET_ALREADY_USED        (8002, "Ve nay da duoc su dung",                  HttpStatus.CONFLICT),
    TICKET_CANCELLED           (8003, "Ve nay da bi huy",                        HttpStatus.CONFLICT),
    INVALID_QR_CODE            (8004, "Ma QR ve khong hop le",                   HttpStatus.BAD_REQUEST),
    TICKET_NOT_ACTIVE          (8005, "Ve hien khong con hieu luc",              HttpStatus.CONFLICT),
    TICKET_CHECKIN_TOO_EARLY   (8006, "Chua den thoi gian mo soat ve",           HttpStatus.CONFLICT),
    TICKET_CHECKIN_EXPIRED     (8007, "Da het thoi gian soat ve",                HttpStatus.CONFLICT),
    TICKET_QR_REQUIRED         (8010, "Vui long cung cap ma QR cua ve",          HttpStatus.BAD_REQUEST),
    TICKET_CHECKIN_CONTEXT_REQUIRED (8011, "Vui long chon rap va suat chieu can soat", HttpStatus.BAD_REQUEST),
    TICKET_WRONG_CINEMA        (8012, "Ve khong thuoc rap dang soat",             HttpStatus.CONFLICT),
    TICKET_WRONG_SHOWTIME      (8013, "Ve khong thuoc suat chieu dang soat",     HttpStatus.CONFLICT),
    BASE_PRICE_REQUIRED        (5013, "Vui long nhap gia ve co ban",              HttpStatus.BAD_REQUEST),
    START_TIME_FUTURE          (5014, "Gio bat dau phai nam trong tuong lai",    HttpStatus.BAD_REQUEST),
    END_TIME_FUTURE            (5015, "Gio ket thuc phai nam trong tuong lai",   HttpStatus.BAD_REQUEST),
    BASE_PRICE_INVALID         (5016, "Gia ve co ban khong hop le",               HttpStatus.BAD_REQUEST),
    
    // ── Payment ───────────────────────────────────────────────────────────
    INVALID_SECURE_TOKEN       (9001, "Ma bao mat khong hop le hoac da het han", HttpStatus.BAD_REQUEST),
    PAYMENT_AMOUNT_MISMATCH    (9002, "So tien thanh toan khong khop voi don hang", HttpStatus.BAD_REQUEST),
    PAYMENT_METHOD_UNAVAILABLE (9003, "Phuong thuc thanh toan hien khong kha dung", HttpStatus.BAD_REQUEST),
    PAYMENT_INVALID_CALLBACK   (9004, "Chu ky callback thanh toan khong hop le", HttpStatus.BAD_REQUEST),
    PAYMENT_PROVIDER_ERROR     (9005, "Cong thanh toan dang gap su co",          HttpStatus.BAD_GATEWAY),
    PAYMENT_IN_PROGRESS        (9006, "Đơn này đang có một giao dịch chờ thanh toán. Vui lòng hoàn tất hoặc chờ giao dịch hết hạn trước khi đổi phương thức.", HttpStatus.CONFLICT),
    REFUND_NOT_FOUND           (9010, "Khong tim thay yeu cau hoan tien",       HttpStatus.NOT_FOUND),
    REFUND_ALREADY_FINALIZED   (9011, "Yeu cau hoan tien da duoc xu ly",         HttpStatus.CONFLICT),
    REFUND_FAILURE_REASON_REQUIRED(9012, "Vui long nhap ly do hoan tien that bai", HttpStatus.BAD_REQUEST),
    ;

    ErrorCode(int code, String message, HttpStatusCode statusCode) {
        this.code       = code;
        this.message    = message;
        this.statusCode = statusCode;
    }

    private final int           code;
    private final String        message;
    private final HttpStatusCode statusCode;
}
