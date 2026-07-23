from __future__ import annotations

from datetime import UTC, datetime
from pathlib import Path
from zipfile import ZIP_DEFLATED, ZipFile
from xml.sax.saxutils import escape


OUT = Path(__file__).with_name("Huong_dan_kien_truc_va_luong_hoat_dong_CinemaBookingSystem.docx")


def x(value: object) -> str:
    return escape(str(value), {'"': "&quot;"})


def p(text: str = "", style: str | None = None, bold: bool = False, italic: bool = False,
      center: bool = False, page_break_before: bool = False) -> str:
    ppr: list[str] = []
    if style:
        ppr.append(f'<w:pStyle w:val="{style}"/>')
    if center:
        ppr.append('<w:jc w:val="center"/>')
    if page_break_before:
        ppr.append('<w:pageBreakBefore/>')

    rpr: list[str] = []
    if bold:
        rpr.append("<w:b/>")
    if italic:
        rpr.append("<w:i/>")

    ppr_xml = f"<w:pPr>{''.join(ppr)}</w:pPr>" if ppr else ""
    rpr_xml = f"<w:rPr>{''.join(rpr)}</w:rPr>" if rpr else ""
    if not text:
        return f"<w:p>{ppr_xml}</w:p>"
    return (
        f"<w:p>{ppr_xml}<w:r>{rpr_xml}"
        f'<w:t xml:space="preserve">{x(text)}</w:t></w:r></w:p>'
    )


def h1(text: str, page_break: bool = True) -> str:
    return p(text, "Heading1", page_break_before=page_break)


def h2(text: str) -> str:
    return p(text, "Heading2")


def h3(text: str) -> str:
    return p(text, "Heading3")


def bullet(text: str, level: int = 0) -> str:
    left = 540 + level * 360
    return (
        "<w:p><w:pPr>"
        f'<w:ind w:left="{left}" w:hanging="260"/>'
        '<w:spacing w:after="80"/>'
        "</w:pPr><w:r><w:t xml:space=\"preserve\">- "
        f"{x(text)}</w:t></w:r></w:p>"
    )


def code(text: str) -> str:
    lines = text.splitlines() or [text]
    return "".join(p(line, "CodeBlock") for line in lines)


def table(rows: list[list[str]], widths: list[int] | None = None) -> str:
    if not rows:
        return ""
    col_count = max(len(row) for row in rows)
    if widths is None:
        widths = [9360 // col_count] * col_count
    widths = (widths + [widths[-1]] * col_count)[:col_count]
    grid = "".join(f'<w:gridCol w:w="{width}"/>' for width in widths)
    xml = [
        "<w:tbl>",
        "<w:tblPr>"
        '<w:tblW w:w="9360" w:type="dxa"/>'
        '<w:tblInd w:w="120" w:type="dxa"/>'
        '<w:tblBorders><w:top w:val="single" w:sz="4" w:color="DADCE0"/>'
        '<w:left w:val="single" w:sz="4" w:color="DADCE0"/>'
        '<w:bottom w:val="single" w:sz="4" w:color="DADCE0"/>'
        '<w:right w:val="single" w:sz="4" w:color="DADCE0"/>'
        '<w:insideH w:val="single" w:sz="4" w:color="DADCE0"/>'
        '<w:insideV w:val="single" w:sz="4" w:color="DADCE0"/></w:tblBorders>'
        '<w:tblCellMar><w:top w:w="80" w:type="dxa"/><w:left w:w="120" w:type="dxa"/>'
        '<w:bottom w:w="80" w:type="dxa"/><w:right w:w="120" w:type="dxa"/></w:tblCellMar>'
        "</w:tblPr>",
        f"<w:tblGrid>{grid}</w:tblGrid>",
    ]
    for row_index, row in enumerate(rows):
        xml.append("<w:tr>")
        for col_index in range(col_count):
            text = row[col_index] if col_index < len(row) else ""
            fill = '<w:shd w:fill="E8EEF5"/>' if row_index == 0 else ""
            bold = "<w:b/>" if row_index == 0 else ""
            xml.append(
                "<w:tc><w:tcPr>"
                f'<w:tcW w:w="{widths[col_index]}" w:type="dxa"/>{fill}'
                "</w:tcPr><w:p><w:r><w:rPr>"
                f'{bold}</w:rPr><w:t xml:space="preserve">{x(text)}</w:t></w:r></w:p></w:tc>'
            )
        xml.append("</w:tr>")
    xml.append("</w:tbl>")
    return "".join(xml)


def section(title: str, paragraphs: list[str]) -> list[str]:
    output = [h1(title)]
    output.extend(p(paragraph) for paragraph in paragraphs)
    return output


annotation_rows = [
    ["Annotation", "Nằm ở đâu", "Ý nghĩa dễ hiểu"],
    ["@SpringBootApplication", "CinemaBookingSystemApplication", "Điểm khởi động Spring Boot. Nó bật auto configuration, component scan và cấu hình mặc định."],
    ["@EnableScheduling", "CinemaBookingSystemApplication", "Cho phép chạy các job tự động như nhả ghế HOLD hết hạn, expire booking pending, dọn invalid token."],
    ["@Configuration", "SecurityConfig, WebSocketConfig, VNPayConfig...", "Khai báo class cấu hình Spring. Spring sẽ đọc class này khi app khởi động."],
    ["@ConfigurationProperties", "JwtProperties, VNPayConfig, MomoConfig, GoogleOAuthProperties", "Map biến trong application.yaml/.env vào object Java có type rõ ràng."],
    ["@Bean", "SecurityConfig, AsyncConfig, ApplicationInitConfig", "Tạo object do Spring quản lý, ví dụ PasswordEncoder hoặc SecurityFilterChain."],
    ["@RestController", "Các controller", "Class nhận HTTP request và trả JSON response."],
    ["@GetMapping/@PostMapping/@PutMapping/@DeleteMapping", "Controller methods", "Gắn method Java với endpoint HTTP cụ thể."],
    ["@PreAuthorize", "Controller methods", "Chặn quyền theo permission trước khi method chạy."],
    ["@Service", "Các service impl", "Đánh dấu class xử lý nghiệp vụ chính."],
    ["@Repository", "Các repository", "Đánh dấu class/interface truy vấn database."],
    ["@Entity/@Table", "Các entity", "Map class Java với bảng database."],
    ["@ManyToOne/@OneToMany/@ManyToMany", "Entity relationships", "Mô tả quan hệ database giữa các bảng."],
    ["@Transactional", "Service methods", "Bọc nhiều thao tác DB vào một giao dịch. Lỗi thì rollback."],
    ["@Transactional(readOnly = true)", "Các method đọc", "Tối ưu cho truy vấn đọc, không cần tracking thay đổi."],
    ["@Transactional(REQUIRES_NEW)", "Audit/event service", "Tạo transaction riêng để log vẫn lưu được dù nghiệp vụ chính rollback."],
    ["@Async", "EmailServiceImpl", "Gửi email ở thread khác để không làm user chờ request lâu."],
    ["@Scheduled", "Scheduler classes", "Chạy định kỳ theo fixedDelay hoặc cron."],
    ["@EnableWebSocketMessageBroker", "WebSocketConfig", "Bật STOMP WebSocket broker để publish seat status realtime."],
]


backend_class_rows = [
    ["Nhóm", "Class quan trọng", "Vai trò"],
    ["Entry point", "CinemaBookingSystemApplication", "Khởi động app và bật scheduler."],
    ["Security", "SecurityConfig", "Cấu hình public endpoint, JWT resource server, PasswordEncoder, method security."],
    ["Security", "CustomJwtDecoder", "Giải mã và kiểm tra JWT, đảm bảo token hợp lệ và chưa bị invalidated."],
    ["Security", "JwtAuthenticationEntryPoint", "Trả lỗi 401 chuẩn khi request chưa xác thực."],
    ["Security", "AuthenticationService", "Đăng nhập username/password, Google login, refresh token, logout, sinh JWT."],
    ["Security task", "TokenCleanupTask", "Dọn invalidated token hết hạn để bảng không phình mãi."],
    ["RBAC seed", "ApplicationInitConfig", "Seed roles, permissions, admin/staff/user mặc định khi app chạy."],
    ["Audit", "AdminAuditLogInterceptor", "Chặn sau request admin để ghi nhật ký thao tác."],
    ["Audit", "AdminAuditLogServiceImpl", "Lưu audit log vào bảng admin_audit_logs bằng transaction riêng."],
    ["Movie", "MovieController/MovieServiceImpl", "Quản lý phim, lọc phim, sắp xếp phim theo booking/revenue."],
    ["Cinema", "CinemaController/CinemaServiceImpl", "Quản lý rạp, map data, rạp gần nhất."],
    ["Room", "RoomController/RoomServiceImpl", "Quản lý phòng chiếu thuộc rạp."],
    ["Seat", "SeatController/SeatServiceImpl", "Tạo ghế đơn lẻ hoặc sinh hàng loạt theo template."],
    ["Showtime", "ShowtimeController/ShowtimeServiceImpl", "Tạo lịch chiếu, kiểm tra trùng phòng, lấy suất mở check-in."],
    ["Booking", "BookingController/BookingServiceImpl", "Giữ ghế, tạo booking, áp mã, hủy, expire, sinh ticket, check-in."],
    ["Payment", "PaymentController/PaymentServiceImpl", "Khởi tạo payment, xử lý VNPay/MoMo callback, reconciliation."],
    ["Payment gateway", "PaymentGateway, VnPayPaymentGateway, MomoPaymentGateway", "Tách logic từng cổng thanh toán để dễ thêm cổng mới."],
    ["Payment audit", "PaymentEventServiceImpl", "Ghi nhật ký payment callback, lỗi chữ ký, amount mismatch, success/failed."],
    ["Email", "EmailServiceImpl", "Gửi email xác thực, reset password, email vé kèm QR."],
    ["Analytics", "AnalyticsController/AnalyticsServiceImpl", "Dashboard doanh thu, vé bán, top phim, export CSV."],
    ["WebSocket", "WebSocketConfig", "Mở endpoint /ws, /ws-native và broker /topic."],
    ["WebSocket", "SeatStatusPublisher", "Publish event HOLD/BOOKED/AVAILABLE theo showtime."],
    ["Schedulers", "HoldExpireScheduler, PendingBookingExpireScheduler", "Nhả ghế giữ quá hạn và expire booking chờ thanh toán."],
    ["Exception", "GlobalExceptionHandler, AppException, ErrorCode", "Chuẩn hóa lỗi trả về client."],
]


entity_rows = [
    ["Entity", "Bảng", "Giải thích nghiệp vụ"],
    ["User", "users", "Tài khoản khách, staff, admin; có email, avatar, trạng thái xác thực email, token reset password."],
    ["Role", "roles", "Vai trò ADMIN/STAFF/USER."],
    ["Permission", "permissions", "Quyền chi tiết như MOVIE_VIEW, PAYMENT_VIEW_ALL, TICKET_CHECKIN."],
    ["Movie", "movies", "Thông tin phim, poster, trailer, trạng thái NOW_SHOWING/COMING_SOON/ENDED."],
    ["Cinema", "cinemas", "Rạp chiếu có địa chỉ, city, latitude/longitude cho map."],
    ["Room", "rooms", "Phòng chiếu thuộc một rạp."],
    ["Seat", "seats", "Ghế vật lý trong phòng, có row/number/type/multiplier."],
    ["Showtime", "showtimes", "Một phim chiếu ở một phòng trong khoảng thời gian cụ thể."],
    ["SeatStatus", "seat_status", "Trạng thái của một ghế trong một suất: AVAILABLE/HOLD/BOOKED."],
    ["Booking", "bookings", "Đơn đặt vé, trạng thái PENDING/SUCCESS/FAILED/CANCELLED/EXPIRED."],
    ["BookingDetail", "booking_details", "Mỗi dòng là một ghế trong booking, lưu giá tại thời điểm đặt."],
    ["Payment", "payments", "Giao dịch thanh toán theo booking, method, transactionNo, status."],
    ["PaymentEvent", "payment_events", "Audit trail cho payment callback và trạng thái gateway."],
    ["Ticket", "tickets", "Vé điện tử sinh sau payment success, có QR unique và trạng thái ACTIVE/USED/CANCELLED."],
    ["Promotion", "promotions", "Mã giảm giá theo PERCENT/FIXED, giới hạn thời gian và số lượt."],
    ["InvalidatedToken", "invalidated_token", "Token đã logout để không dùng lại được."],
    ["AdminAuditLog", "admin_audit_logs", "Nhật ký thao tác admin/staff trên API quan trọng."],
]


frontend_rows = [
    ["Nhóm", "File/Class", "Vai trò"],
    ["Entry", "main.tsx, App.tsx", "Mount React app, provider, router, toast."],
    ["Routing", "AppRouter.tsx", "Chia route public, auth, user, staff, admin và lazy load page."],
    ["Security UI", "ProtectedRoute.tsx", "Chặn trang theo login/permission phía client."],
    ["State", "authStore.ts", "Lưu token, user, permission vào localStorage bằng Zustand."],
    ["API core", "axiosClient.ts", "Gắn Authorization Bearer token, logout khi 401 ngoài auth flow."],
    ["API modules", "movieApi, bookingApi, paymentApi...", "Mỗi module bọc một nhóm endpoint backend."],
    ["Public pages", "HomePage, MovieDetailPage, CinemaMapPage, CinemaDetailPage", "Khách xem phim, rạp, map, lịch chiếu."],
    ["User pages", "SeatSelectionPage, CheckoutPage, MyBookingsPage, TicketDetailPage, ProfilePage", "Luồng đặt vé và quản lý vé cá nhân."],
    ["Admin pages", "AdminDashboardPage, AdminMoviePage, AdminPaymentPage...", "Quản trị hệ thống."],
    ["Staff", "StaffTicketScannerPage", "Quét QR bằng camera hoặc file ảnh, gửi cinemaId/showtimeId để soát đúng cửa."],
    ["Realtime", "useSeatWebSocket.ts", "Subscribe topic seatmap và cập nhật trạng thái ghế realtime."],
    ["UI", "BrandLogo, MovieCard, QuickBookingWidget, Toast", "Component tái sử dụng."],
]


controller_rows = [
    ["Controller", "Endpoint chính", "Permission/ghi chú"],
    ["AuthenticationController", "/auth/token, /auth/google, /auth/refresh, /auth/logout", "Public endpoint, xử lý đăng nhập và token."],
    ["UserController", "/api/v1/users/register, /me, /{id}, /block", "Public register/verify; admin quản lý user; user cập nhật profile."],
    ["MovieController", "/api/v1/movies", "Public xem phim; admin/staff quản lý theo permission movie."],
    ["CinemaController", "/api/v1/cinemas, /map, /nearest", "Public xem rạp/map; admin quản lý rạp."],
    ["RoomController", "/api/v1/rooms", "Quản lý phòng theo rạp."],
    ["SeatController", "/api/v1/seats, /bulk-generate", "Quản lý ghế, sinh sơ đồ ghế."],
    ["ShowtimeController", "/api/v1/showtimes", "Quản lý suất chiếu, public xem lịch chiếu."],
    ["BookingController", "/api/v1/bookings/hold, /bookings, /tickets/check-in", "Giữ ghế, tạo booking, áp mã, xem vé, staff check-in."],
    ["PaymentController", "/api/v1/payments/initiate, /vnpay-callback, /events, /reconciliation", "Thanh toán, callback, admin audit/đối soát."],
    ["PromotionController", "/api/v1/promotions", "Quản lý và xem mã khuyến mãi."],
    ["AnalyticsController", "/api/v1/analytics/summary, /revenue/*, /export", "Dashboard và export báo cáo."],
    ["AdminAuditLogController", "/api/v1/admin/audit-logs", "Xem nhật ký thao tác admin/staff."],
]


flow_summaries = [
    ("Luồng đăng ký và xác thực email", [
        "Frontend RegisterPage gửi username, email, password đến UserController.register.",
        "UserServiceImpl kiểm tra username/email trùng, mã hóa password bằng BCryptPasswordEncoder.",
        "User được tạo với role USER và emailVerified=false.",
        "Service tạo token xác thực email dạng raw token, lưu bản hash vào user để DB không lưu token thô.",
        "EmailServiceImpl.sendEmailVerification chạy @Async, render email-verification.html và gửi SMTP.",
        "Người dùng bấm link xác thực, frontend gọi /api/v1/users/verify-email.",
        "UserServiceImpl hash token nhận được, so sánh token hash và expiry, sau đó set emailVerified=true.",
    ]),
    ("Luồng đăng nhập username/password", [
        "LoginPage gọi authApi.login, backend nhận ở AuthenticationController.token.",
        "AuthenticationService.authenticate tìm user theo username, kiểm tra password bằng BCrypt.",
        "Nếu user bị khóa, xóa mềm hoặc sai mật khẩu, backend ném AppException với ErrorCode phù hợp.",
        "Nếu hợp lệ, AuthenticationService.generateToken tạo JWT chứa subject userId/username và scope permission.",
        "Frontend lưu token, user, permissions vào authStore/localStorage.",
        "axiosClient tự gắn Authorization header cho các request tiếp theo.",
    ]),
    ("Luồng Google login", [
        "Frontend nhận Google credential/token từ Google Identity và gọi /auth/google.",
        "AuthenticationService.authenticateWithGoogle xác thực thông tin Google bằng client-id cấu hình.",
        "Nếu email đã có user thì cập nhật avatar/name nếu phù hợp; nếu chưa có thì tạo user mới.",
        "User Google thường được đánh dấu emailVerified=true vì email đã được Google xác thực.",
        "Backend trả JWT giống luồng đăng nhập thường.",
    ]),
    ("Luồng xem phim và lịch chiếu public", [
        "HomePage gọi movieApi/cinemaApi/showtimeApi qua React Query.",
        "SecurityConfig permit GET /api/v1/movies/**, /api/v1/showtimes/**, /api/v1/cinemas/** nên chưa đăng nhập vẫn xem được.",
        "MovieServiceImpl và ShowtimeServiceImpl lọc soft delete, trạng thái phim/suất chiếu và public-days-ahead.",
        "Frontend hiển thị phim, rạp, lịch chiếu. Chỉ khi bấm chọn ghế mới cần login.",
    ]),
    ("Luồng giữ ghế realtime", [
        "SeatSelectionPage mở sơ đồ ghế và useSeatWebSocket subscribe /topic/seatmap/{showtimeId}.",
        "User chọn ghế, frontend gọi BookingController.holdSeats.",
        "BookingServiceImpl kiểm tra user, showtime, cutoff time, danh sách seatId.",
        "SeatStatusRepository khóa các dòng seat_status bằng lock để tránh hai user giữ cùng ghế.",
        "Nếu ghế AVAILABLE hoặc HOLD của chính user còn hợp lệ, backend set status=HOLD, holdBy=currentUser, holdUntil=now+seatHoldMinutes.",
        "SeatStatusPublisher.publishHold gửi SeatStatusEvent qua WebSocket.",
        "Các client đang xem cùng showtime tự đổi màu ghế mà không cần refresh.",
    ]),
    ("Luồng tạo booking", [
        "Sau khi hold ghế, frontend gọi BookingController.createBooking.",
        "BookingServiceImpl kiểm tra các ghế đang HOLD bởi chính user và chưa hết hạn.",
        "Tính tổng tiền = basePrice * priceMultiplier từng ghế, trừ discount nếu có.",
        "Tạo Booking status=PENDING, secureToken unique, paymentExpiresAt=now+pendingTimeoutMinutes.",
        "Tạo BookingDetail cho từng ghế, giữ priceAtBooking để lịch sử không đổi khi giá sau này đổi.",
        "Ghế vẫn ở HOLD cho tới khi payment success/failure/expire.",
    ]),
    ("Luồng áp mã giảm giá", [
        "CheckoutPage gửi promotionCode vào BookingController.applyPromotion.",
        "BookingServiceImpl kiểm tra booking thuộc user hiện tại, status=PENDING.",
        "PromotionServiceImpl kiểm tra code active, chưa hết hạn, min order, usage limit.",
        "Giảm giá được tính theo PERCENT hoặc FIXED, có maxDiscountAmount nếu cấu hình.",
        "Booking cập nhật promotion, discountAmount, totalPrice.",
    ]),
    ("Luồng thanh toán VNPay", [
        "CheckoutPage gọi PaymentController.initiate với bookingId, method=VNPAY, amount.",
        "PaymentServiceImpl kiểm tra booking PENDING, chưa hết paymentExpiresAt, amount khớp totalPrice.",
        "Nếu đã có payment PENDING cùng method thì reuse để tránh tạo nhiều giao dịch vô ích.",
        "PaymentGateway được chọn theo method. VnPayPaymentGateway tạo URL thanh toán có chữ ký hashSecret.",
        "Người dùng thanh toán ở VNPay sandbox, VNPay redirect về /api/v1/payments/vnpay-callback.",
        "PaymentServiceImpl xác thực chữ ký, transactionNo, amount, responseCode.",
        "Thành công thì gọi BookingServiceImpl.handlePaymentSuccess; thất bại thì handlePaymentFailure.",
        "PaymentEventServiceImpl ghi audit event ở các mốc: initiated, url created, callback received, invalid signature, amount mismatch, success/failed.",
    ]),
    ("Luồng payment success", [
        "BookingServiceImpl.handlePaymentSuccess tìm booking theo secureToken.",
        "Nếu booking đã SUCCESS thì trả về idempotent, không tạo vé trùng.",
        "Cập nhật booking.status=SUCCESS.",
        "Cập nhật các seat_status từ HOLD sang BOOKED.",
        "Tạo Ticket cho mỗi BookingDetail, mỗi ghế một QR riêng để check-in từng vé.",
        "SeatStatusPublisher.publishBulk(...BOOKED) đẩy realtime cho các client khác.",
        "Sau commit transaction, EmailServiceImpl.sendTicketEmail gửi email vé có thông tin rạp, địa chỉ, ghế, QR.",
    ]),
    ("Luồng payment failed hoặc booking expired", [
        "Payment failed: callback báo thất bại hoặc user hủy flow thanh toán.",
        "BookingServiceImpl.handlePaymentFailure set booking.status=FAILED nếu còn PENDING.",
        "SeatStatus của các ghế booking được trả về AVAILABLE.",
        "PendingBookingExpireScheduler chạy định kỳ, tìm booking PENDING quá paymentExpiresAt và gọi expirePendingBooking.",
        "expirePendingBooking set booking.status=EXPIRED, payment pending liên quan có thể EXPIRED, nhả ghế AVAILABLE.",
        "Frontend nhận realtime AVAILABLE và người khác có thể đặt lại ghế.",
    ]),
    ("Luồng soát vé QR", [
        "StaffTicketScannerPage yêu cầu staff chọn thành phố/rạp/suất chiếu đang mở check-in.",
        "Staff quét camera hoặc upload ảnh QR.",
        "Frontend gửi qrCode, cinemaId, showtimeId tới BookingController.checkInTicket.",
        "BookingServiceImpl.checkInTicket kiểm tra QR hợp lệ, ticket ACTIVE, booking SUCCESS.",
        "Backend kiểm tra vé thuộc đúng cinemaId và đúng showtimeId trước khi set USED.",
        "Backend kiểm tra cửa sổ check-in: sớm tối đa ticket.check-in-early-minutes và muộn tối đa ticket.check-in-late-minutes.",
        "Nếu hợp lệ thì set ticket.status=USED, checkInTime=now, checkedInBy=staff.",
    ]),
    ("Luồng admin quản trị", [
        "AdminLayout hiển thị sidebar các module theo permission.",
        "ProtectedRoute kiểm tra permission phía client để ẩn/chặn trang.",
        "Backend vẫn là lớp bảo vệ chính bằng @PreAuthorize ở controller.",
        "Admin thêm/sửa/xóa phim, rạp, phòng, ghế, suất chiếu, mã khuyến mãi, user.",
        "Admin xem dashboard analytics, payment list, payment events, reconciliation và audit logs.",
        "AdminAuditLogInterceptor ghi log sau request để biết ai thao tác gì, endpoint nào, status code nào.",
    ]),
]


parts: list[str] = []
parts.extend([
    p("cinemabooking.vn", "Subtitle", center=True),
    p("SỔ TAY KIẾN TRÚC VÀ LUỒNG HOẠT ĐỘNG HỆ THỐNG", "Title", bold=True, center=True),
    p("Cinema Booking System - Spring Boot backend + React frontend", center=True),
    p(f"Cập nhật: {datetime.now().strftime('%d/%m/%Y %H:%M')}", center=True),
    p(),
    table([
        ["Mục", "Nội dung"],
        ["Mục tiêu", "Giải thích toàn bộ kiến trúc, class, annotation, config và các luồng nghiệp vụ chính để đọc code dễ hơn."],
        ["Phạm vi", "Backend cinema-booking-system, frontend cinema-client, database SQL, migration, mock data, payment, email, websocket, admin."],
        ["Lưu ý", "Tài liệu này chỉ là file hướng dẫn, không được ứng dụng import hoặc chạy khi start hệ thống."],
    ], [2200, 7160]),
    p("", page_break_before=True),
])

parts += section("1. Cách Nhìn Tổng Quan", [
    "Hãy tưởng tượng hệ thống như một cụm rạp số. Frontend React là quầy vé và màn hình tự phục vụ mà khách nhìn thấy. Backend Spring Boot là phòng điều hành phía sau, nơi kiểm tra quyền, giữ ghế, tính tiền, gọi cổng thanh toán, sinh vé và gửi thông báo. PostgreSQL là sổ cái chính thức lưu mọi sự thật của hệ thống.",
    "Một request bình thường đi theo đường: React page -> api module -> axiosClient -> Spring Controller -> Service -> Repository -> PostgreSQL. Nếu có thay đổi quan trọng như giữ ghế hoặc thanh toán, backend còn phát WebSocket event để các trình duyệt khác cập nhật ngay.",
    "Nguyên tắc quan trọng nhất: frontend giúp trải nghiệm mượt, nhưng backend mới là nơi quyết định cuối cùng. Vì vậy dù client có hiển thị ghế trống, backend vẫn kiểm tra lock, trạng thái ghế, user, permission, payment và thời gian trước khi chấp nhận.",
])

parts.extend([
    h2("1.1. Bản đồ lớp lớn"),
    table([
        ["Lớp", "Ví dụ", "Nhiệm vụ"],
        ["Controller", "BookingController, PaymentController", "Nhận HTTP request, kiểm tra permission qua annotation, gọi service."],
        ["Service", "BookingServiceImpl, PaymentServiceImpl", "Xử lý nghiệp vụ, transaction, kiểm tra trạng thái, gọi repository/gateway/email/websocket."],
        ["Repository", "BookingRepository, SeatStatusRepository", "Truy vấn database bằng Spring Data JPA, JPQL hoặc native query."],
        ["Entity", "Booking, SeatStatus, Ticket", "Map object Java với bảng database."],
        ["DTO", "CreateBookingRequest, BookingResponse", "Định dạng dữ liệu vào/ra API, không lộ trực tiếp entity."],
        ["Mapper", "BookingMapper, PaymentMapper", "Chuyển entity sang response DTO."],
        ["Config", "SecurityConfig, WebSocketConfig", "Cấu hình security, websocket, async, gateway, OpenAPI."],
        ["Frontend page", "SeatSelectionPage, CheckoutPage", "Màn hình người dùng thao tác."],
        ["Frontend api", "bookingApi, paymentApi", "Bọc axios call theo từng module nghiệp vụ."],
    ], [1600, 2900, 4860]),
    h2("1.2. Quy tắc đọc code nhanh"),
])
for item in [
    "Muốn biết endpoint nào nhận request: mở package controller.",
    "Muốn biết nghiệp vụ thật chạy ra sao: mở service/impl tương ứng.",
    "Muốn biết dữ liệu lưu thế nào: mở entity và database/database.sql.",
    "Muốn biết query có tối ưu không: mở repository và migration/index trong database.",
    "Muốn biết quyền ai được gọi endpoint: xem @PreAuthorize và ApplicationInitConfig/RBAC file.",
    "Muốn biết frontend gọi endpoint ở đâu: tìm trong src/api rồi lần ngược về page dùng React Query hoặc mutation.",
]:
    parts.append(bullet(item))

parts.extend([
    h1("2. Cấu Trúc Backend Spring Boot"),
    p("Backend được tổ chức theo kiểu nhiều tầng. Controller không xử lý nghiệp vụ nặng. Service là nơi quyết định trạng thái và transaction. Repository chỉ lo truy vấn. Mapper/DTO giúp API sạch và ổn định."),
    table(backend_class_rows, [1700, 3100, 4560]),
    h2("2.1. Entry point"),
    p("CinemaBookingSystemApplication là class khởi động. Annotation @SpringBootApplication bảo Spring scan các component trong package com.cinema.booking. Annotation @EnableScheduling bật các job chạy nền như nhả ghế hết hạn."),
    code("""@SpringBootApplication
@EnableScheduling
public class CinemaBookingSystemApplication {
    public static void main(String[] args) {
        SpringApplication.run(CinemaBookingSystemApplication.class, args);
    }
}"""),
    h2("2.2. Controller layer"),
    p("Controller là cổng HTTP. Ví dụ BookingController nhận request giữ ghế, tạo booking, xem booking, áp mã giảm giá và check-in vé. Controller thường không tự xử lý database mà gọi BookingService."),
    table(controller_rows, [2100, 3800, 3460]),
    h2("2.3. Service layer"),
    p("Service là não nghiệp vụ. Đây là nơi đặt @Transactional để đảm bảo nhiều update database đi cùng nhau. Ví dụ payment success phải cập nhật booking, seat_status, ticket, payment event và publish websocket. Nếu một bước lỗi, transaction giúp rollback dữ liệu liên quan."),
    p("Các service quan trọng nhất là BookingServiceImpl và PaymentServiceImpl. BookingServiceImpl xử lý hold seat, create booking, payment success/failure, cancel, expire, ticket và check-in. PaymentServiceImpl xử lý initiate payment, callback VNPay/MoMo, audit event và reconciliation."),
    h2("2.4. Repository layer"),
    p("Repository là lớp truy vấn database. Spring Data JPA tự sinh nhiều query theo tên method, nhưng hệ thống cũng dùng JPQL/native query cho các phần hiệu năng cao như seat hold, dashboard, reconciliation. Các query nhạy cảm như giữ ghế phải lock đúng dòng seat_status để tránh race condition."),
    h2("2.5. Mapper và DTO"),
    p("DTO giúp client nhận dữ liệu vừa đủ. Entity thường chứa quan hệ lazy, field nhạy cảm và cấu trúc nội bộ. Mapper chuyển entity sang response, ví dụ BookingResponse có movieTitle, cinemaName, roomName, seats, tickets để frontend hiển thị mà không cần tự join nhiều API."),
])

parts.extend([
    h1("3. Annotation Và Config Quan Trọng"),
    p("Phần này giải thích các annotation thường gặp khi đọc code. Nếu hiểu nhóm annotation này, bạn sẽ đọc Spring Boot dễ hơn rất nhiều."),
    table(annotation_rows, [2300, 2700, 4360]),
    h2("3.1. SecurityConfig"),
    p("SecurityConfig quyết định endpoint nào public, endpoint nào cần JWT, và permission trong token được map thành authority thế nào. Public GET cho phim/rạp/lịch chiếu giúp người chưa đăng nhập vẫn xem được thông tin. Các thao tác đặt vé, admin, staff phải có token."),
    code("""httpSecurity.authorizeHttpRequests(request -> request
    .requestMatchers(PUBLIC_WS_ENDPOINTS).permitAll()
    .requestMatchers(SWAGGER_ENDPOINTS).permitAll()
    .requestMatchers(HttpMethod.POST, PUBLIC_POST_ENDPOINTS).permitAll()
    .requestMatchers(HttpMethod.GET, PUBLIC_GET_ENDPOINTS).permitAll()
    .anyRequest().authenticated());"""),
    p("JwtGrantedAuthoritiesConverter được set authorityPrefix rỗng. Nghĩa là nếu JWT có scope MOVIE_VIEW thì Spring thấy authority đúng là MOVIE_VIEW, không bị thêm prefix SCOPE_. Vì vậy @PreAuthorize có thể viết hasAuthority('MOVIE_VIEW')."),
    h2("3.2. application.yaml"),
    p("application.yaml là nơi khai báo datasource, Flyway, JPA, mail, VNPay, MoMo, booking timeout, showtime public window, frontend URL, JWT và ticket QR. Hầu hết giá trị đều đọc từ biến môi trường .env để dễ đổi giữa local/test/product."),
    table([
        ["Nhóm config", "Biến quan trọng", "Ý nghĩa"],
        ["Database", "DB_HOST, DB_PORT_EXTERNAL, DB_NAME", "Kết nối PostgreSQL."],
        ["Flyway", "FLYWAY_ENABLED, locations", "Chạy migration tạo/cập nhật schema."],
        ["JPA", "ddl-auto=validate, open-in-view=false", "Không tự sửa schema, tránh lazy load ngoài transaction."],
        ["Booking", "BOOKING_SEAT_HOLD_MINUTES, BOOKING_PENDING_TIMEOUT_MINUTES", "Thời gian giữ ghế và chờ thanh toán."],
        ["Payment", "VNP_*, MOMO_*", "Cấu hình cổng thanh toán."],
        ["Ticket", "TICKET_QR_SECRET, TICKET_CHECK_IN_EARLY_MINUTES", "Ký QR và cửa sổ check-in."],
        ["Mail", "MAIL_HOST, MAIL_USERNAME, MAIL_PASSWORD", "SMTP gửi email xác thực/vé/reset password."],
    ], [2000, 3200, 4160]),
    h2("3.3. Flyway và database"),
    p("Flyway đọc các file trong src/main/resources/db/migration. V1 tạo schema chính, V2 thêm index tối ưu lookup payment pending, V3 tạo bảng payment_events. File database/database.sql là bản tổng hợp dễ đọc/chạy tay, còn Flyway là cơ chế chuẩn khi app tự migrate."),
])

parts.extend([
    h1("4. Database Và Entity"),
    p("Database là nơi lưu sự thật cuối cùng. Frontend có thể cache, backend có thể có object trong RAM, nhưng trạng thái chính thức của ghế, booking, payment, ticket vẫn nằm ở PostgreSQL."),
    table(entity_rows, [1800, 2300, 5260]),
    h2("4.1. Quan hệ chính"),
])
for item in [
    "Cinema có nhiều Room.",
    "Room có nhiều Seat.",
    "Movie có nhiều Showtime.",
    "Showtime thuộc một Movie và một Room.",
    "SeatStatus nối Seat với Showtime để biết ghế đó trong suất đó đang AVAILABLE, HOLD hay BOOKED.",
    "Booking thuộc User và Showtime, có nhiều BookingDetail.",
    "BookingDetail nối Booking với Seat và có một Ticket sau khi thanh toán thành công.",
    "Payment thuộc Booking, có PaymentEvent để ghi lịch sử vận hành.",
    "User có nhiều Role, Role có nhiều Permission.",
]:
    parts.append(bullet(item))

parts.extend([
    h2("4.2. Vì sao cần bảng SeatStatus?"),
    p("Seat là ghế vật lý trong phòng, ví dụ A1 ở RAP 1. Nhưng A1 có thể trống ở suất 9:00 và đã bán ở suất 19:30. Vì vậy không thể lưu trạng thái trực tiếp trong Seat. Bảng SeatStatus tạo một dòng cho từng cặp showtime-seat. Đây là điểm mấu chốt để đặt vé đúng."),
    code("""Seat physical: Room 1 - A1
Showtime 09:00: SeatStatus(A1, showtime09) = AVAILABLE
Showtime 19:30: SeatStatus(A1, showtime19) = BOOKED"""),
    h2("4.3. Vì sao mỗi ghế một QR?"),
    p("Một booking có thể mua nhiều ghế. Product thực tế thường sinh mỗi vé/ghế một QR để check-in linh hoạt hơn: một người trong nhóm có thể vào trước, người khác vào sau. Vì vậy Ticket gắn với BookingDetail, không gắn trực tiếp với Booking."),
])

parts.extend([
    h1("5. Luồng Frontend React"),
    p("Frontend được chia theo vai trò: public, user, staff, admin. AppRouter dùng lazy import để trang nào cần mới tải, giúp bundle chính nhẹ hơn. ProtectedRoute kiểm tra login và permission phía client để điều hướng sớm, nhưng backend vẫn là lớp bảo vệ bắt buộc."),
    table(frontend_rows, [1700, 3000, 4660]),
    h2("5.1. axiosClient"),
    p("axiosClient tạo baseURL từ VITE_API_BASE_URL. Khi có token trong authStore, request interceptor tự gắn Authorization. Response interceptor tự logout khi gặp 401 ở các request không phải auth/register/reset password. Nhờ đó token hết hạn hoặc bị invalid thì user quay về login rõ ràng."),
    code("""axiosClient.interceptors.request.use((config) => {
    const token = useAuthStore.getState().token;
    if (token && !config.headers.Authorization) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});"""),
    h2("5.2. authStore"),
    p("authStore dùng Zustand để lưu token, user và permission. Dữ liệu cũng được ghi vào localStorage để refresh trang không mất phiên. hasPermission kiểm tra permission của user để ProtectedRoute và layout quyết định có cho vào trang hay không."),
    h2("5.3. React Query"),
    p("Các page dùng React Query để gọi API, cache dữ liệu, refetch và hiển thị loading/error. Ví dụ AdminPaymentPage gọi paymentApi.getAllPayments, getPaymentEvents, getReconciliationIssues. SeatSelectionPage lấy seat map và kết hợp WebSocket event để cập nhật trạng thái ghế."),
])

parts.append(h1("6. Các Luồng Hoạt Động Chi Tiết"))
for title, steps in flow_summaries:
    parts.append(h2(title))
    for step in steps:
        parts.append(bullet(step))

parts.extend([
    h1("7. WebSocket Realtime Seat Map"),
    p("WebSocket trong hệ thống giống như bảng điện tử trong rạp. Khi một người giữ ghế hoặc thanh toán xong, backend phát tin tới tất cả người đang xem cùng suất chiếu. Không ai cần bấm refresh."),
    h2("7.1. Server mở kênh"),
    code("""registry.enableSimpleBroker("/topic");
registry.setApplicationDestinationPrefixes("/app");
registry.addEndpoint("/ws").setAllowedOriginPatterns("*").withSockJS();
registry.addEndpoint("/ws-native").setAllowedOriginPatterns("*");"""),
    p("Frontend có thể kết nối /ws-native cho browser hiện đại hoặc /ws với SockJS fallback. Topic ghế là /topic/seatmap/{showtimeId}."),
    h2("7.2. Event payload"),
    p("SeatStatusEvent chứa showtimeId, seatIds hoặc seatId, status, heldByUserId, holdUntil. Frontend nhìn status để đổi màu ghế. Nếu heldByUserId là user hiện tại thì ghế có thể hiện trạng thái giữ bởi chính mình."),
    h2("7.3. Các nơi publish event"),
])
for item in [
    "holdSeats: publish HOLD.",
    "handlePaymentSuccess: publish BOOKED.",
    "handlePaymentFailure/cancelBooking/expirePendingBooking: publish AVAILABLE.",
    "HoldExpireScheduler: publish AVAILABLE khi hold_until hết hạn.",
]:
    parts.append(bullet(item))

parts.extend([
    h1("8. Payment, Audit Và Reconciliation"),
    p("Payment được thiết kế để dễ mở rộng cổng thanh toán. Thay vì viết hết VNPay/MoMo trong PaymentServiceImpl, hệ thống có interface PaymentGateway. Mỗi gateway implement supports(method) và createPaymentUrl(...). PaymentServiceImpl chỉ chọn gateway phù hợp."),
    code("""public interface PaymentGateway {
    boolean supports(PaymentMethod method);
    String createPaymentUrl(Payment payment, HttpServletRequest request);
}"""),
    h2("8.1. Vì sao cần PaymentEvent?"),
    p("Thanh toán thật không chỉ có success/fail. Callback có thể đến nhiều lần, sai chữ ký, sai amount, gateway lỗi, user tạo lại payment URL. PaymentEvent là nhật ký kỹ thuật giúp admin/dev biết chuyện gì đã xảy ra theo thời gian."),
    table([
        ["Event", "Khi nào ghi"],
        ["PAYMENT_INITIATED", "User bắt đầu thanh toán."],
        ["PAYMENT_REUSED", "Tái sử dụng payment pending cũ."],
        ["PAYMENT_URL_CREATED", "Tạo URL redirect sang gateway thành công."],
        ["VNPAY_CALLBACK_RECEIVED/MOMO_CALLBACK_RECEIVED", "Gateway gọi callback/return/IPN."],
        ["*_INVALID_SIGNATURE", "Sai chữ ký, không tin callback."],
        ["*_AMOUNT_MISMATCH", "Số tiền gateway trả về không khớp booking."],
        ["PAYMENT_SUCCESS/PAYMENT_FAILED/PAYMENT_EXPIRED", "Trạng thái cuối cùng."],
        ["PAYMENT_PROVIDER_ERROR", "Lỗi khi gọi gateway."],
    ], [3600, 5760]),
    h2("8.2. Reconciliation là gì?"),
    p("Reconciliation là đối soát. Nó tìm các trường hợp lệch như booking SUCCESS nhưng không có payment SUCCESS, payment SUCCESS nhưng booking chưa SUCCESS, booking SUCCESS nhưng chưa có ticket, hoặc booking pending đã hết hạn mà ghế vẫn HOLD. Đây là màn admin dùng để phát hiện lỗi vận hành."),
])

parts.extend([
    h1("9. Exception Và Error Trả Về Client"),
    p("Hệ thống dùng AppException + ErrorCode để ném lỗi nghiệp vụ có kiểm soát. GlobalExceptionHandler bắt lỗi và trả ApiResponse chuẩn. Cách này tốt hơn ném RuntimeException chung vì frontend có thể hiển thị message rõ hơn."),
    table([
        ["Thành phần", "Vai trò"],
        ["ErrorCode", "Danh sách mã lỗi, message, HTTP status."],
        ["AppException", "Exception nghiệp vụ mang ErrorCode."],
        ["GlobalExceptionHandler", "Bắt AppException, validation error, access denied, lỗi hệ thống."],
        ["ApiResponse", "Format JSON thống nhất: code, message, result."],
    ], [2300, 7060]),
    h2("9.1. Ví dụ lỗi check-in"),
])
for item in [
    "QR không hợp lệ: báo mã QR không hợp lệ.",
    "Vé đã dùng: báo đã check-in, kèm thời gian và staff nếu có.",
    "Sai rạp: báo vé không thuộc rạp này.",
    "Sai suất chiếu: báo vé không thuộc suất chiếu đang soát.",
    "Chưa tới giờ check-in: báo chưa đến thời gian mở check-in.",
]:
    parts.append(bullet(item))

parts.extend([
    h1("10. Admin, Staff Và RBAC"),
    p("RBAC trong hệ thống đi theo mô hình User - Role - Permission. User có thể có nhiều role. Role có nhiều permission. JWT chứa permission để backend dùng @PreAuthorize kiểm tra endpoint."),
    table([
        ["Role", "Quyền chính"],
        ["ADMIN", "Toàn quyền quản trị: movie, cinema, room, seat, showtime, booking, payment, user, promotion, analytics, audit."],
        ["STAFF", "Xem dữ liệu vận hành, quản lý một số phần rạp/suất chiếu, check-in vé QR."],
        ["USER", "Xem phim/rạp/lịch, đặt vé, thanh toán, xem vé cá nhân, cập nhật profile."],
    ], [1600, 7760]),
    h2("10.1. Hai lớp bảo vệ"),
])
for item in [
    "Frontend ProtectedRoute giúp UI không cho user vào nhầm trang.",
    "Backend @PreAuthorize mới là bảo vệ thật vì client có thể bị sửa.",
    "SecurityConfig yêu cầu authenticated cho mọi endpoint không public.",
    "JWT decoder kiểm tra token, invalidated token và thời hạn.",
]:
    parts.append(bullet(item))

parts.extend([
    h1("11. Test, Build Và Những Điểm Cần Nhớ Khi Demo"),
    p("Tại lần kiểm tra gần nhất, backend test pass 19 tests và frontend npm run check pass lint, 3 test files, 6 tests, build production. Điều này không thay thế kiểm thử tay end-to-end, nhưng chứng minh code hiện tại build được và các test quan trọng đang xanh."),
    h2("11.1. Checklist demo khuyến nghị"),
])
for item in [
    "Start PostgreSQL, backend, frontend.",
    "Seed database/mock-data nếu cần dữ liệu demo nhanh.",
    "Đăng nhập user1, chọn phim, chọn suất, giữ ghế, áp mã giảm giá.",
    "Thanh toán VNPay sandbox, quay về payment result.",
    "Kiểm tra email vé trong Mailtrap/Gmail cấu hình.",
    "Vào Vé của tôi, mở chi tiết vé.",
    "Đăng nhập staff1, chọn rạp/suất chiếu và quét QR.",
    "Đăng nhập admin, xem dashboard, booking, payment, payment events, reconciliation, audit log.",
]:
    parts.append(bullet(item))

parts.extend([
    h1("12. Những Chỗ Nên Nhớ Khi Bảo Vệ"),
    p("Điểm mạnh nhất của đề tài không chỉ là có nhiều màn hình. Điểm đáng nói là hệ thống giải quyết được các vấn đề product thật: tránh trùng ghế, realtime seat map, timeout giữ ghế, payment callback idempotent, email vé, QR check-in có kiểm tra đúng rạp/suất chiếu, RBAC permission-based và admin payment reconciliation."),
    h2("12.1. Câu trả lời ngắn cho các câu hỏi dễ bị hỏi"),
])
qa_rows = [
    ["Câu hỏi", "Ý trả lời nên nói"],
    ["Vì sao cần SeatStatus?", "Vì một ghế vật lý có trạng thái khác nhau ở từng suất chiếu."],
    ["Vì sao cần lock khi giữ ghế?", "Để hai user không cùng giữ/mua một ghế trong cùng thời điểm."],
    ["Vì sao mỗi ghế một QR?", "Để check-in từng vé linh hoạt và tránh một QR đại diện cả nhóm gây khó soát từng người."],
    ["Vì sao frontend cũng kiểm tra permission?", "Để UX tốt hơn, nhưng backend @PreAuthorize mới là bảo vệ bắt buộc."],
    ["Vì sao có PaymentEvent?", "Để debug và audit callback thanh toán thật, vì gateway có thể callback lỗi/lặp/sai chữ ký."],
    ["Vì sao dùng WebSocket?", "Để người khác thấy ghế đổi trạng thái ngay, không cần refresh."],
    ["Vì sao dùng Flyway?", "Để schema database có version, dễ triển khai và rollback/kiểm soát thay đổi."],
]
parts.append(table(qa_rows, [3100, 6260]))

parts.extend([
    h1("13. Phụ Lục: Lệnh Chạy Và File Cần Biết"),
    table([
        ["Mục", "File/lệnh"],
        ["Backend config", "src/main/resources/application.yaml và .env"],
        ["Database tổng hợp", "database/database.sql"],
        ["Mock data", "database/mock-data.sql"],
        ["Flyway migrations", "src/main/resources/db/migration"],
        ["RBAC document", "database/rbac-permissions.sql"],
        ["Frontend env", "cinema-client/.env"],
        ["Backend test", ".\\mvnw.cmd test"],
        ["Frontend check", "npm run check"],
        ["Frontend dev", "npm run dev"],
    ], [2600, 6760]),
    h2("13.1. Kết luận"),
    p("Tài liệu này là bản đọc code có hệ thống. Khi cần hiểu một bug hoặc một flow, hãy tìm theo flow trước, sau đó mở các class đã liệt kê trong bảng. Cách học nhanh nhất là chạy demo, đặt breakpoint ở Controller/Service chính, rồi quan sát database thay đổi ở từng bước."),
])


def styles_xml() -> str:
    return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:styles xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
  <w:docDefaults><w:rPrDefault><w:rPr><w:rFonts w:ascii="Calibri" w:hAnsi="Calibri"/><w:sz w:val="22"/></w:rPr></w:rPrDefault><w:pPrDefault><w:pPr><w:spacing w:after="120" w:line="300" w:lineRule="auto"/></w:pPr></w:pPrDefault></w:docDefaults>
  <w:style w:type="paragraph" w:default="1" w:styleId="Normal"><w:name w:val="Normal"/><w:qFormat/><w:pPr><w:spacing w:after="120" w:line="300" w:lineRule="auto"/></w:pPr><w:rPr><w:rFonts w:ascii="Calibri" w:hAnsi="Calibri"/><w:sz w:val="22"/></w:rPr></w:style>
  <w:style w:type="paragraph" w:styleId="Title"><w:name w:val="Title"/><w:qFormat/><w:pPr><w:spacing w:before="0" w:after="160"/><w:jc w:val="center"/></w:pPr><w:rPr><w:rFonts w:ascii="Calibri" w:hAnsi="Calibri"/><w:b/><w:color w:val="0B2545"/><w:sz w:val="40"/></w:rPr></w:style>
  <w:style w:type="paragraph" w:styleId="Subtitle"><w:name w:val="Subtitle"/><w:qFormat/><w:pPr><w:spacing w:after="80"/><w:jc w:val="center"/></w:pPr><w:rPr><w:rFonts w:ascii="Calibri" w:hAnsi="Calibri"/><w:color w:val="6B7280"/><w:sz w:val="24"/></w:rPr></w:style>
  <w:style w:type="paragraph" w:styleId="Heading1"><w:name w:val="Heading 1"/><w:basedOn w:val="Normal"/><w:next w:val="Normal"/><w:qFormat/><w:pPr><w:keepNext/><w:spacing w:before="360" w:after="200"/></w:pPr><w:rPr><w:rFonts w:ascii="Calibri" w:hAnsi="Calibri"/><w:b/><w:color w:val="2E74B5"/><w:sz w:val="32"/></w:rPr></w:style>
  <w:style w:type="paragraph" w:styleId="Heading2"><w:name w:val="Heading 2"/><w:basedOn w:val="Normal"/><w:next w:val="Normal"/><w:qFormat/><w:pPr><w:keepNext/><w:spacing w:before="240" w:after="120"/></w:pPr><w:rPr><w:rFonts w:ascii="Calibri" w:hAnsi="Calibri"/><w:b/><w:color w:val="2E74B5"/><w:sz w:val="26"/></w:rPr></w:style>
  <w:style w:type="paragraph" w:styleId="Heading3"><w:name w:val="Heading 3"/><w:basedOn w:val="Normal"/><w:next w:val="Normal"/><w:qFormat/><w:pPr><w:keepNext/><w:spacing w:before="160" w:after="80"/></w:pPr><w:rPr><w:rFonts w:ascii="Calibri" w:hAnsi="Calibri"/><w:b/><w:color w:val="1F4D78"/><w:sz w:val="24"/></w:rPr></w:style>
  <w:style w:type="paragraph" w:styleId="CodeBlock"><w:name w:val="Code Block"/><w:basedOn w:val="Normal"/><w:pPr><w:spacing w:before="0" w:after="0"/><w:ind w:left="360"/><w:shd w:fill="F3F4F6"/></w:pPr><w:rPr><w:rFonts w:ascii="Consolas" w:hAnsi="Consolas"/><w:sz w:val="18"/><w:color w:val="111827"/></w:rPr></w:style>
</w:styles>"""


def document_xml(body_parts: list[str]) -> str:
    body = "".join(body_parts)
    return f"""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
  <w:body>
    {body}
    <w:sectPr>
      <w:pgSz w:w="12240" w:h="15840"/>
      <w:pgMar w:top="1440" w:right="1440" w:bottom="1440" w:left="1440" w:header="708" w:footer="708" w:gutter="0"/>
    </w:sectPr>
  </w:body>
</w:document>"""


def write_docx() -> None:
    now_utc = datetime.now(UTC).replace(tzinfo=None).isoformat() + "Z"
    content_types = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
  <Override PartName="/word/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml"/>
  <Override PartName="/docProps/core.xml" ContentType="application/vnd.openxmlformats-package.core-properties+xml"/>
  <Override PartName="/docProps/app.xml" ContentType="application/vnd.openxmlformats-officedocument.extended-properties+xml"/>
</Types>"""
    rels = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>"""
    doc_rels = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rIdStyles" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>"""
    core = f"""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<cp:coreProperties xmlns:cp="http://schemas.openxmlformats.org/package/2006/metadata/core-properties" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:dcterms="http://purl.org/dc/terms/" xmlns:dcmitype="http://purl.org/dc/dcmitype/" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <dc:title>Huong dan kien truc va luong hoat dong Cinema Booking System</dc:title>
  <dc:creator>Codex</dc:creator>
  <cp:lastModifiedBy>Codex</cp:lastModifiedBy>
  <dcterms:created xsi:type="dcterms:W3CDTF">{now_utc}</dcterms:created>
  <dcterms:modified xsi:type="dcterms:W3CDTF">{now_utc}</dcterms:modified>
</cp:coreProperties>"""
    app = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Properties xmlns="http://schemas.openxmlformats.org/officeDocument/2006/extended-properties" xmlns:vt="http://schemas.openxmlformats.org/officeDocument/2006/docPropsVTypes">
  <Application>Codex</Application>
</Properties>"""

    with ZipFile(OUT, "w", ZIP_DEFLATED) as z:
        z.writestr("[Content_Types].xml", content_types)
        z.writestr("_rels/.rels", rels)
        z.writestr("word/_rels/document.xml.rels", doc_rels)
        z.writestr("word/document.xml", document_xml(parts))
        z.writestr("word/styles.xml", styles_xml())
        z.writestr("docProps/core.xml", core)
        z.writestr("docProps/app.xml", app)


if __name__ == "__main__":
    write_docx()
    print(OUT)
