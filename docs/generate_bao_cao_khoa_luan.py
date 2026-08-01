from __future__ import annotations

from datetime import UTC, datetime
from pathlib import Path
from zipfile import ZipFile, ZIP_DEFLATED
from xml.sax.saxutils import escape


OUT = Path(__file__).with_name("Bao_cao_khoa_luan_cinema_booking_system.docx")


def x(text: object) -> str:
    return escape(str(text), {"\"": "&quot;"})


def p(text: str = "", style: str | None = None, bold: bool = False, italic: bool = False,
      center: bool = False, page_break_before: bool = False) -> str:
    ppr = []
    if style:
        ppr.append(f'<w:pStyle w:val="{style}"/>')
    if center:
        ppr.append('<w:jc w:val="center"/>')
    if page_break_before:
        ppr.append('<w:pageBreakBefore/>')
    rpr = []
    if bold:
        rpr.append("<w:b/>")
    if italic:
        rpr.append("<w:i/>")
    rpr_xml = f"<w:rPr>{''.join(rpr)}</w:rPr>" if rpr else ""
    ppr_xml = f"<w:pPr>{''.join(ppr)}</w:pPr>" if ppr else ""
    if text == "":
        return f"<w:p>{ppr_xml}</w:p>"
    return f"<w:p>{ppr_xml}<w:r>{rpr_xml}<w:t xml:space=\"preserve\">{x(text)}</w:t></w:r></w:p>"


def bullet(text: str, level: int = 0) -> str:
    indent = 720 + level * 360
    return (
        '<w:p><w:pPr>'
        f'<w:ind w:left="{indent}" w:hanging="360"/>'
        '</w:pPr><w:r><w:t xml:space="preserve">• '
        f'{x(text)}</w:t></w:r></w:p>'
    )


def code(text: str) -> str:
    return p(text, "CodeBlock")


def table(rows: list[list[str]], widths: list[int] | None = None) -> str:
    if not rows:
        return ""
    cols = max(len(r) for r in rows)
    if widths is None:
        widths = [9000 // cols] * cols
    grid = "".join(f'<w:gridCol w:w="{w}"/>' for w in widths[:cols])
    out = [
        '<w:tbl>',
        '<w:tblPr><w:tblStyle w:val="TableGrid"/>'
        '<w:tblW w:w="0" w:type="auto"/>'
        '<w:tblLook w:val="04A0" w:firstRow="1" w:lastRow="0" w:firstColumn="1" '
        'w:lastColumn="0" w:noHBand="0" w:noVBand="1"/></w:tblPr>',
        f'<w:tblGrid>{grid}</w:tblGrid>',
    ]
    for i, row in enumerate(rows):
        out.append("<w:tr>")
        for j, cell in enumerate(row):
            shade = '<w:shd w:fill="D9EAF7"/>' if i == 0 else ""
            bold = "<w:b/>" if i == 0 else ""
            out.append(
                '<w:tc><w:tcPr>'
                f'<w:tcW w:w="{widths[min(len(widths)-1, j)]}" w:type="dxa"/>{shade}'
                '</w:tcPr><w:p><w:r><w:rPr>'
                f'{bold}</w:rPr><w:t xml:space="preserve">{x(cell)}</w:t></w:r></w:p></w:tc>'
            )
        out.append("</w:tr>")
    out.append("</w:tbl>")
    return "".join(out)


def section(title: str, paras: list[str]) -> list[str]:
    parts = [p(title, "Heading1", page_break_before=True)]
    parts.extend(p(t) if t else p() for t in paras)
    return parts


def h2(title: str) -> str:
    return p(title, "Heading2")


def h3(title: str) -> str:
    return p(title, "Heading3")


tech_rows = [
    ["Nhóm", "Công nghệ/Thành phần", "Vai trò trong hệ thống"],
    ["Backend", "Java 21, Spring Boot 3.5.14", "Xây dựng REST API, xử lý nghiệp vụ đặt vé, thanh toán, phân quyền."],
    ["Persistence", "PostgreSQL, Spring Data JPA, Hibernate", "Lưu trữ dữ liệu phim, rạp, phòng, ghế, suất chiếu, booking, ticket, payment."],
    ["Security", "Spring Security, OAuth2 Resource Server, JWT, BCrypt", "Xác thực đăng nhập, phát token, kiểm soát quyền theo RBAC."],
    ["Realtime", "WebSocket/STOMP, native WS và SockJS endpoint", "Đẩy trạng thái ghế HOLD/BOOKED/AVAILABLE tới nhiều client cùng lúc."],
    ["Email", "Spring Mail, Thymeleaf", "Gửi email vé sau thanh toán thành công, render nội dung xác nhận vé."],
    ["API Docs", "SpringDoc OpenAPI/Swagger UI", "Cung cấp tài liệu API để kiểm thử và tích hợp frontend."],
    ["Frontend", "React 19, TypeScript, Vite 8, React Router 7", "Xây dựng giao diện public, user, staff, admin."],
    ["Frontend State", "Zustand, React Query, Axios", "Quản lý phiên đăng nhập, cache dữ liệu, gọi API."],
    ["UI/UX", "Tailwind CSS 4, lucide-react, Recharts, Leaflet", "Giao diện responsive, dashboard biểu đồ, bản đồ rạp."],
    ["QR", "html5-qrcode", "Quét mã vé tại màn hình nhân viên."],
    ["DevOps local", "Docker Compose PostgreSQL, Maven, npm", "Khởi tạo môi trường phát triển và chạy dự án cục bộ."],
]

backend_modules = [
    ["Thư mục", "Số file", "Ý nghĩa"],
    ["controller", "12", "Lớp REST API cho movie, cinema, room, seat, showtime, booking, payment, ticket, analytics, auth, user, promotion."],
    ["service / impl", "23", "Định nghĩa interface và hiện thực nghiệp vụ chính."],
    ["entity", "16", "Mô hình JPA ánh xạ các bảng nghiệp vụ và RBAC."],
    ["dto/request, dto/response", "47", "Chuẩn hóa dữ liệu vào/ra, giảm lộ entity ra ngoài API."],
    ["repository", "14", "Truy vấn dữ liệu bằng Spring Data JPA và native query cho thống kê."],
    ["mapper", "12", "Chuyển đổi Entity sang DTO và ngược lại."],
    ["security", "5", "Cấu hình SecurityFilterChain, JWT decoder, entry point, scheduler token và hold."],
    ["websocket", "3", "Cấu hình broker, event DTO, publisher trạng thái ghế realtime."],
    ["configuration", "5", "Cấu hình OpenAPI, JWT properties, async, VNPay và dữ liệu khởi tạo."],
]

frontend_modules = [
    ["Thư mục", "Số file", "Ý nghĩa"],
    ["pages/public", "5", "Trang chủ, chi tiết phim, bản đồ rạp, đăng nhập, đăng ký."],
    ["pages/user", "6", "Chọn ghế, thanh toán, kết quả thanh toán, vé của tôi, chi tiết vé, hồ sơ."],
    ["pages/admin", "6", "Dashboard, quản lý phim, rạp, suất chiếu, người dùng, modal phim."],
    ["pages/staff", "1", "Màn hình quét QR check-in vé."],
    ["api", "10", "Các module gọi API cho auth, movie, cinema, booking, payment, ticket, user, analytics."],
    ["components/layout", "5", "Navbar, footer, public layout, auth layout, admin layout."],
    ["stores", "3", "Lưu token, user, permission, theme bằng Zustand/localStorage."],
    ["hooks", "2", "Debounce và kết nối WebSocket trạng thái ghế."],
    ["types", "2", "Định nghĩa kiểu dữ liệu domain và API dùng chung."],
]

api_rows = [
    ["Nhóm API", "Endpoint chính", "Tình trạng/chức năng"],
    ["Auth", "POST /auth/token, /auth/refresh, /auth/logout, /auth/introspect", "Đã có đăng nhập, refresh, logout và kiểm tra token."],
    ["User", "/api/v1/users/register, /me, /{id}, /{id}/block", "Đăng ký, xem/sửa hồ sơ, admin quản lý và khóa/mở khóa user."],
    ["Movie", "/api/v1/movies", "CRUD phim, lọc/phân trang, xem chi tiết public."],
    ["Cinema", "/api/v1/cinemas, /map, /nearest", "CRUD rạp, dữ liệu bản đồ và tìm rạp gần nhất."],
    ["Room", "/api/v1/rooms/cinema/{cinemaId}", "CRUD phòng theo rạp."],
    ["Seat", "/api/v1/seats, /bulk-generate", "Tạo ghế đơn lẻ, sinh hàng loạt, cập nhật/xóa ghế có kiểm tra đang dùng."],
    ["Showtime", "/api/v1/showtimes, /movie/{movieId}, /cinema/{cinemaId}, /{id}/seats", "CRUD suất chiếu, truy vấn theo phim/rạp, lấy sơ đồ ghế."],
    ["Booking", "/api/v1/bookings/hold, /bookings, /my, /{id}/cancel", "Giữ ghế 10 phút, tạo booking, xem đơn, hủy đơn pending."],
    ["Payment", "/api/v1/payments/initiate, /vnpay-callback, /my", "Khởi tạo VNPay/mock, xác thực callback, cập nhật booking/ticket."],
    ["Ticket", "/api/v1/tickets/my, /check-in, /tickets", "Xem vé cá nhân, staff check-in bằng QR, admin/staff xem tất cả."],
    ["Analytics", "/api/v1/analytics/summary, /revenue/daily, /monthly, /movies/top-revenue, /showtimes", "Dashboard doanh thu, booking, ticket, top phim và hiệu suất suất chiếu."],
]

entity_rows = [
    ["Entity", "Thuộc tính chính", "Quan hệ/ghi chú"],
    ["User", "username, password, firstName, lastName, dob, phone, email, isActive, isDeleted", "Many-to-many với Role, dùng cho khách hàng, nhân viên, admin."],
    ["Role", "name, description", "Many-to-many với Permission, đại diện ADMIN/STAFF/USER."],
    ["Permission", "name, description", "Danh sách quyền chi tiết theo module."],
    ["Movie", "title, description, duration, genre, releaseDate, posterUrl, status, director, actors, language, ageRating, ratingImdb", "Một phim có nhiều suất chiếu."],
    ["Cinema", "name, address, city, latitude, longitude, isActive", "Một rạp có nhiều phòng; hỗ trợ bản đồ."],
    ["Room", "cinema, name, isDeleted", "Một phòng có nhiều ghế và nhiều suất chiếu."],
    ["Seat", "room, rowLabel, seatNumber, rowIndex, colIndex, seatType, priceMultiplier", "Unique theo phòng/hàng/số ghế; seatType NORMAL/VIP/COUPLE."],
    ["Showtime", "movie, room, startTime, endTime, basePrice, status", "Sinh SeatStatus để theo dõi trạng thái từng ghế theo suất."],
    ["SeatStatus", "showtime, seat, status, holdBy, holdUntil", "Unique theo showtime-seat, lock pessimistic khi giữ ghế."],
    ["Booking", "user, showtime, promotion, totalPrice, discountAmount, status, secureToken", "Một booking có nhiều BookingDetail."],
    ["BookingDetail", "booking, seat, priceAtBooking", "Lưu giá ghế tại thời điểm đặt, liên kết Ticket."],
    ["Ticket", "bookingDetail, qrCode, status, checkInTime", "QR unique, ACTIVE/USED/CANCELLED."],
    ["Payment", "booking, amount, method, transactionNo, status, paymentTime, gatewayResponse", "Ghi nhận thanh toán VNPay/mock."],
    ["Promotion", "code, discountType, discountValue, maxDiscountAmount, minOrderValue, date range, usageLimit, usedCount", "Áp mã giảm giá theo phần trăm hoặc số tiền cố định."],
    ["InvalidatedToken", "id, expiryTime", "Danh sách token đã logout/thu hồi."],
]

progress_rows = [
    ["Hạng mục", "Mức hoàn thiện hiện tại", "Nhận xét"],
    ["Backend CRUD cốt lõi", "Hoàn thành ở mức tốt", "Có đủ movie/cinema/room/seat/showtime/user/promotion với validation và phân quyền."],
    ["Luồng đặt vé", "Hoàn thành luồng chính", "Có giữ ghế, tạo booking, thanh toán thành công/thất bại, hủy pending, sinh vé."],
    ["Chống trùng ghế", "Đã xử lý trọng tâm", "Dùng PESSIMISTIC_WRITE trên SeatStatus khi hold/create booking."],
    ["Realtime", "Đã tích hợp", "WebSocket publish khi HOLD/BOOKED/AVAILABLE, frontend subscribe theo showtime."],
    ["Thanh toán", "Đã có VNPay sandbox/callback và mock fallback", "Cần kiểm thử thực tế nhiều case gateway và xử lý redirect frontend hoàn thiện hơn."],
    ["Email", "Đã có", "Gửi email vé bất đồng bộ bằng Thymeleaf template; cần cấu hình Mailtrap/SMTP thật."],
    ["QR check-in", "Đã có", "Ticket QR được sinh sau payment success; staff page dùng html5-qrcode."],
    ["Dashboard", "Đã có nền tảng", "Backend analytics và frontend Recharts; cần tinh chỉnh báo cáo nghiệp vụ sâu hơn."],
    ["Frontend public/user", "Đã có luồng sử dụng chính", "Trang chủ, chi tiết phim, chọn ghế, checkout, vé của tôi, profile, bản đồ rạp."],
    ["Frontend admin/staff", "Đã có các màn vận hành quan trọng", "Quản lý phim/rạp/suất chiếu/user và quét QR."],
    ["Kiểm thử tự động", "Mới ở mức cơ bản", "Đã có contextLoads; cần bổ sung unit/integration test cho booking/payment/security."],
    ["Triển khai production", "Chưa hoàn thiện", "Mới có Docker Compose PostgreSQL local; chưa có pipeline CI/CD, container backend/frontend production."],
]

test_rows = [
    ["Loại kiểm thử", "Hiện trạng", "Kết quả/đề xuất"],
    ["Backend Maven test", "Đã chạy ngày 14/07/2026 bằng lệnh mvn test", "BUILD SUCCESS, Tests run: 1, Failures: 0, Errors: 0, Skipped: 0."],
    ["Context load", "Có test mặc định CinemaBookingSystemApplicationTests.contextLoads", "Xác nhận application context khởi động được trong môi trường có dependency và DB phù hợp."],
    ["Unit test service", "Chưa thấy test chuyên sâu", "Cần test BookingServiceImpl: hold, createBooking, success, failure, cancel."],
    ["Integration test API", "Chưa thấy test controller/API", "Cần test endpoint với MockMvc/Testcontainers/PostgreSQL."],
    ["Frontend test", "Chưa thấy cấu hình test runner", "Có thể thêm Vitest/React Testing Library cho component và flow quan trọng."],
    ["E2E", "Chưa có", "Có thể dùng Playwright để test luồng đăng nhập, chọn ghế, thanh toán mock, check-in."],
]


chapters: list[str] = []
chapters += [
    p("TRƯỜNG/ KHOA: ................................................", center=True),
    p("BÁO CÁO KHÓA LUẬN TỐT NGHIỆP", "Title", bold=True, center=True),
    p("ĐỀ TÀI", center=True, bold=True),
    p("XÂY DỰNG HỆ THỐNG ĐẶT VÉ XEM PHIM TRỰC TUYẾN", "Title", bold=True, center=True),
    p("Dự án: cinema-booking-system và cinema-client", center=True),
    p(),
    table([
        ["Thông tin", "Nội dung"],
        ["Sinh viên thực hiện", "................................................"],
        ["Mã sinh viên", "................................................"],
        ["Lớp", "................................................"],
        ["Giảng viên hướng dẫn", "................................................"],
        ["Thời điểm lập báo cáo", "14/07/2026"],
        ["Phạm vi báo cáo", "Mô tả chi tiết hiện trạng mã nguồn backend Spring Boot và frontend React hiện có."],
    ], [2800, 6200]),
    p(),
    p("TP. Hồ Chí Minh/Hà Nội, 2026", center=True),
    p("", page_break_before=True),
    p("LỜI CAM ĐOAN", "Heading1"),
    p("Em xin cam đoan báo cáo này được xây dựng dựa trên việc khảo sát trực tiếp mã nguồn hiện tại của hai phần dự án: backend cinema-booking-system và frontend cinema-client. Các phần mô tả chức năng, kiến trúc, cơ sở dữ liệu, luồng nghiệp vụ, tiến độ thực hiện và định hướng phát triển đều phản ánh đúng trạng thái dự án tại thời điểm 14/07/2026. Những phần còn thiếu hoặc chưa hoàn thiện được trình bày rõ trong chương đánh giá hiện trạng và hướng phát triển, không cố tình mô tả vượt quá phạm vi mã nguồn đang có."),
    p("Báo cáo có mục đích phục vụ học tập, trình bày kết quả thực hiện khóa luận và làm cơ sở để giảng viên đánh giá tiến độ phát triển hệ thống. Trong quá trình hoàn thiện tiếp theo, nội dung báo cáo có thể được cập nhật thêm hình ảnh giao diện, sơ đồ UML chính thức, kết quả kiểm thử chi tiết và số liệu triển khai thực tế."),
    p("LỜI CẢM ƠN", "Heading1", page_break_before=True),
    p("Em xin gửi lời cảm ơn đến giảng viên hướng dẫn đã định hướng, góp ý và hỗ trợ em trong quá trình lựa chọn đề tài, phân tích yêu cầu, xây dựng kiến trúc và triển khai hệ thống đặt vé xem phim trực tuyến. Đề tài này giúp em có cơ hội vận dụng nhiều kiến thức đã học như lập trình hướng đối tượng, thiết kế cơ sở dữ liệu, phát triển ứng dụng web, bảo mật API, xử lý giao dịch, giao tiếp thời gian thực và tổ chức mã nguồn theo mô hình nhiều tầng."),
    p("Trong quá trình thực hiện, em nhận thấy việc xây dựng một hệ thống đặt vé không chỉ là tạo các màn hình chọn phim và thanh toán, mà còn phải giải quyết nhiều vấn đề thực tế như tránh đặt trùng ghế, quản lý thời gian giữ ghế, xác thực người dùng, phân quyền nhân viên, gửi vé điện tử, quét mã QR, thống kê doanh thu và đảm bảo trải nghiệm người dùng mượt mà. Những yêu cầu này giúp đề tài có tính ứng dụng cao và tạo nền tảng để em tiếp tục hoàn thiện theo hướng sản phẩm thực tế."),
    p("TÓM TẮT ĐỀ TÀI", "Heading1", page_break_before=True),
    p("Đề tài xây dựng hệ thống đặt vé xem phim trực tuyến gồm hai phần chính: backend cinema-booking-system phát triển bằng Spring Boot và frontend cinema-client phát triển bằng React, TypeScript và Vite. Hệ thống hướng tới việc hỗ trợ khách hàng tra cứu phim, xem suất chiếu, chọn ghế theo sơ đồ, giữ ghế trong thời gian ngắn, tạo booking, thanh toán, nhận vé điện tử và dùng vé để check-in tại rạp. Đồng thời, hệ thống cung cấp các chức năng dành cho nhân viên và quản trị viên như quản lý phim, rạp, suất chiếu, người dùng, kiểm tra vé bằng QR và xem thống kê vận hành."),
    p("Backend hiện đã có cấu trúc tương đối đầy đủ theo mô hình controller-service-repository, sử dụng JPA Entity, DTO, mapper, exception handler, JWT security, RBAC, WebSocket, VNPay, email và analytics. Frontend hiện đã có nhiều màn hình theo vai trò public, user, staff và admin, có API client, lưu trạng thái xác thực bằng Zustand, dùng React Query cho dữ liệu, dùng WebSocket để nhận thay đổi ghế theo thời gian thực, dùng Leaflet cho bản đồ rạp và html5-qrcode để quét vé."),
    p("Kết quả hiện tại cho thấy hệ thống đã hoàn thành phần lớn luồng nghiệp vụ cốt lõi của một nền tảng đặt vé xem phim. Tuy nhiên, dự án vẫn cần bổ sung kiểm thử tự động chuyên sâu, hoàn thiện tài liệu vận hành, chuẩn hóa quy trình triển khai production, tăng cường giám sát lỗi, kiểm thử bảo mật và kiểm thử tải đối với các luồng có cạnh tranh tài nguyên như giữ ghế và thanh toán."),
    p("MỤC LỤC TÓM TẮT", "Heading1", page_break_before=True),
]

toc_items = [
    "Chương 1. Tổng quan đề tài",
    "Chương 2. Cơ sở lý thuyết và công nghệ sử dụng",
    "Chương 3. Khảo sát và phân tích yêu cầu",
    "Chương 4. Thiết kế kiến trúc hệ thống",
    "Chương 5. Thiết kế cơ sở dữ liệu",
    "Chương 6. Phân tích backend cinema-booking-system",
    "Chương 7. Phân tích frontend cinema-client",
    "Chương 8. Luồng nghiệp vụ chi tiết",
    "Chương 9. Bảo mật, phân quyền và an toàn dữ liệu",
    "Chương 10. Kiểm thử, đánh giá hiện trạng và tiến độ",
    "Chương 11. Hướng phát triển và kết luận",
]
chapters.extend(bullet(item) for item in toc_items)


chapters += section("CHƯƠNG 1. TỔNG QUAN ĐỀ TÀI", [
    "Trong những năm gần đây, nhu cầu đặt vé xem phim trực tuyến ngày càng phổ biến do thói quen sử dụng điện thoại, máy tính và các dịch vụ số tăng mạnh. Người dùng mong muốn có thể xem lịch chiếu, chọn vị trí ghế, thanh toán và nhận vé điện tử mà không cần xếp hàng tại quầy. Đối với rạp chiếu phim, hệ thống trực tuyến giúp giảm tải nhân viên, quản lý doanh thu minh bạch hơn, cập nhật trạng thái ghế nhanh hơn và thu thập dữ liệu để phân tích hành vi khách hàng.",
    "Đề tài cinema-booking-system và cinema-client được lựa chọn nhằm mô phỏng một hệ thống đặt vé xem phim hoàn chỉnh. Điểm quan trọng của đề tài không chỉ nằm ở thao tác CRUD đơn giản, mà còn ở luồng nghiệp vụ có ràng buộc thời gian thực: nhiều người có thể xem cùng một suất chiếu, chọn cùng một ghế và thực hiện thanh toán gần như đồng thời. Vì vậy, hệ thống phải có cơ chế giữ ghế, khóa dữ liệu, cập nhật realtime và xử lý thanh toán thành công/thất bại một cách nhất quán.",
    "Backend cinema-booking-system là phần trung tâm xử lý nghiệp vụ. Backend chịu trách nhiệm xác thực người dùng, phân quyền, quản lý dữ liệu phim/rạp/phòng/ghế/suất chiếu, giữ ghế, tạo booking, tích hợp thanh toán, sinh vé QR, gửi email, thống kê và phát sự kiện WebSocket. Frontend cinema-client là phần giao diện người dùng, giúp khách hàng tương tác với hệ thống và giúp admin/staff vận hành các chức năng quản trị.",
    "Tại thời điểm lập báo cáo, dự án đã vượt qua giai đoạn khởi tạo ban đầu và đã có một tập hợp chức năng khá đầy đủ. Backend có 154 file Java trong src/main/java, frontend có 56 file trong src, bao gồm nhiều trang chức năng thực tế. Dự án đã có Docker Compose cho PostgreSQL, có mock-data.sql để tạo dữ liệu mẫu nghiệp vụ, có cấu hình Vite proxy để frontend gọi backend, và có test backend context load chạy thành công bằng Maven.",
])

chapters += [
    h2("1.1. Lý do chọn đề tài"),
    p("Đặt vé xem phim là một bài toán phù hợp cho khóa luận vì kết hợp được nhiều kiến thức quan trọng của phát triển phần mềm hiện đại: thiết kế cơ sở dữ liệu quan hệ, xây dựng API REST, xác thực bằng JWT, phân quyền theo vai trò, xử lý giao dịch, đồng bộ trạng thái realtime, tích hợp thanh toán, gửi email và xây dựng giao diện responsive. Đây là đề tài có nghiệp vụ quen thuộc nhưng vẫn đủ độ khó để thể hiện năng lực phân tích, thiết kế và cài đặt."),
    p("So với các đề tài quản lý thông tin thuần túy, hệ thống đặt vé có yêu cầu về tính nhất quán dữ liệu cao hơn. Một ghế trong một suất chiếu chỉ được bán cho một người; trạng thái ghế phải thay đổi theo thời gian; booking có thể ở trạng thái pending, success, failed hoặc cancelled; payment callback có thể đến muộn hoặc lặp; nhân viên chỉ được check-in vé hợp lệ. Những vấn đề này tạo cơ hội để áp dụng transaction, lock, state machine và kiểm soát quyền truy cập."),
    h2("1.2. Mục tiêu của đề tài"),
]
for item in [
    "Xây dựng backend REST API có khả năng quản lý toàn bộ dữ liệu vận hành rạp chiếu phim.",
    "Xây dựng frontend web cho khách hàng, nhân viên và quản trị viên.",
    "Hỗ trợ luồng đặt vé gồm xem phim, xem suất chiếu, chọn ghế, giữ ghế, tạo booking, thanh toán và nhận vé.",
    "Đảm bảo trạng thái ghế được cập nhật theo thời gian thực để giảm rủi ro đặt trùng.",
    "Áp dụng bảo mật JWT và phân quyền RBAC cho từng nhóm người dùng.",
    "Tích hợp thanh toán VNPay ở mức sandbox/callback và hỗ trợ mock gateway cho quá trình phát triển.",
    "Tạo nền tảng thống kê doanh thu, vé bán, top phim và hiệu quả suất chiếu.",
    "Xây dựng báo cáo hiện trạng chi tiết, chỉ rõ phần đã làm, phần còn thiếu và lộ trình hoàn thiện.",
]:
    chapters.append(bullet(item))

chapters += [
    h2("1.3. Phạm vi thực hiện hiện tại"),
    p("Phạm vi hiện tại gồm hai ứng dụng tách biệt. Backend chạy tại port 8080, dùng PostgreSQL làm cơ sở dữ liệu. Frontend chạy bằng Vite tại port 5173 và dùng proxy để chuyển tiếp các request /api, /auth, /ws, /ws-native sang backend. Dự án chưa phải là một hệ thống production hoàn chỉnh, nhưng đã có đủ thành phần để chạy thử end-to-end trong môi trường local development."),
    p("Các chức năng quản trị đã bao gồm quản lý phim, rạp, suất chiếu và người dùng. Chức năng quản lý phòng và ghế có backend đầy đủ hơn frontend; frontend admin hiện tập trung vào các màn vận hành chính. Chức năng khuyến mãi có backend CRUD và áp dụng khi tạo booking; frontend có thể cần bổ sung màn quản lý riêng để admin thao tác thuận tiện. Phần payment có VNPay URL/callback và mock fallback; cần kiểm thử thêm với môi trường sandbox thực tế."),
]

chapters += section("CHƯƠNG 2. CƠ SỞ LÝ THUYẾT VÀ CÔNG NGHỆ SỬ DỤNG", [
    "Hệ thống được xây dựng theo mô hình client-server. Frontend chịu trách nhiệm hiển thị giao diện, nhận thao tác người dùng và gọi API. Backend chịu trách nhiệm xử lý nghiệp vụ, truy cập cơ sở dữ liệu, kiểm tra quyền, giao tiếp với cổng thanh toán, gửi email và phát sự kiện realtime. Cách tổ chức này giúp tách biệt phần trình bày và phần xử lý nghiệp vụ, thuận lợi cho bảo trì và mở rộng.",
    "Ở backend, Spring Boot là nền tảng phù hợp vì hỗ trợ mạnh mẽ cho REST API, validation, security, JPA, transaction, scheduling, mail, WebSocket và OpenAPI. Việc dùng Java 21 giúp dự án tận dụng phiên bản JDK hiện đại, trong khi Spring Boot 3.5.14 cung cấp hệ sinh thái ổn định. PostgreSQL được chọn vì là hệ quản trị cơ sở dữ liệu quan hệ mạnh, hỗ trợ constraint, index, transaction và kiểu jsonb dùng cho dữ liệu phản hồi gateway.",
    "Ở frontend, React kết hợp TypeScript giúp xây dựng giao diện component-based, tăng khả năng tái sử dụng và giảm lỗi kiểu dữ liệu. Vite giúp khởi động nhanh trong môi trường development. React Router định tuyến nhiều nhóm trang; React Query hỗ trợ quản lý trạng thái server; Zustand lưu trạng thái client như token, user, permission; Axios chuẩn hóa request/response; Tailwind CSS tạo giao diện responsive.",
])

chapters += [table(tech_rows, [1800, 3300, 3900])]

chapters += [
    h2("2.1. Mô hình REST API"),
    p("REST API được sử dụng để frontend và backend giao tiếp qua HTTP. Mỗi nhóm tài nguyên có một endpoint chính, ví dụ /api/v1/movies cho phim, /api/v1/cinemas cho rạp, /api/v1/showtimes cho suất chiếu và /api/v1/bookings cho đặt vé. Các phương thức GET, POST, PUT, PATCH và DELETE được dùng theo đúng ý nghĩa: lấy dữ liệu, tạo mới, cập nhật toàn bộ/một phần và xóa mềm hoặc xóa logic."),
    p("Hệ thống dùng DTO để tách dữ liệu API khỏi Entity. Request DTO định nghĩa dữ liệu đầu vào cần validation, Response DTO định nghĩa dữ liệu trả về cho client. Cách làm này giúp API ổn định hơn khi cấu trúc database thay đổi và tránh việc lộ trực tiếp các quan hệ lazy hoặc trường nhạy cảm như password."),
    h2("2.2. JWT và RBAC"),
    p("JWT được dùng để xác thực stateless. Sau khi đăng nhập, người dùng nhận access token. Frontend lưu token trong localStorage thông qua Zustand store, sau đó Axios interceptor tự động gắn header Authorization: Bearer token cho các request cần đăng nhập. Backend dùng OAuth2 Resource Server và CustomJwtDecoder để giải mã/kiểm tra token."),
    p("RBAC là mô hình phân quyền dựa trên vai trò và quyền. Dự án có ba role chính: ADMIN, STAFF và USER. Mỗi role được gán nhiều permission cụ thể như MOVIE_CREATE, BOOKING_CREATE, TICKET_CHECKIN, ANALYTICS_VIEW. Controller dùng @PreAuthorize để kiểm soát từng endpoint. Cách phân quyền theo permission chi tiết giúp hệ thống linh hoạt hơn so với chỉ kiểm tra role cứng."),
    h2("2.3. WebSocket và realtime seat map"),
    p("Trong bài toán đặt vé, trạng thái ghế cần cập nhật gần như tức thời. Nếu hai khách hàng cùng mở một suất chiếu, khi người thứ nhất giữ ghế, người thứ hai phải thấy ghế đổi trạng thái để tránh chọn nhầm. Hệ thống dùng WebSocket/STOMP để backend publish sự kiện trạng thái ghế tới topic /topic/seatmap/{showtimeId}. Frontend subscribe đúng topic của suất chiếu hiện tại và cập nhật giao diện."),
    p("Backend có SeatStatusPublisher để phát các sự kiện HOLD, BOOKED và AVAILABLE. Khi hold ghế, event chứa thêm heldByUserId và holdUntil để frontend phân biệt ghế do chính người dùng hiện tại giữ hay do người khác giữ. Khi thanh toán thành công, backend publish BOOKED; khi thanh toán thất bại, hủy đơn hoặc scheduler nhả ghế hết hạn, backend publish AVAILABLE."),
]

chapters += section("CHƯƠNG 3. KHẢO SÁT VÀ PHÂN TÍCH YÊU CẦU", [
    "Hệ thống có ba nhóm tác nhân chính: khách hàng, nhân viên và quản trị viên. Khách hàng là người sử dụng chức năng xem phim, đăng ký/đăng nhập, chọn suất chiếu, chọn ghế, thanh toán và xem vé. Nhân viên là người vận hành tại rạp, có nhiệm vụ kiểm tra vé và check-in bằng mã QR. Quản trị viên là người quản lý nội dung và dữ liệu vận hành như phim, rạp, phòng, ghế, suất chiếu, người dùng, khuyến mãi và thống kê.",
    "Các yêu cầu được phân tích dựa trên mã nguồn hiện tại. Nhiều yêu cầu đã được hiện thực ở backend và frontend; một số yêu cầu có backend nhưng frontend chưa có màn riêng; một số yêu cầu nằm ở hướng phát triển như CI/CD, test coverage, payment gateway production và báo cáo nâng cao.",
])

chapters += [
    h2("3.1. Yêu cầu chức năng phía khách hàng"),
]
for item in [
    "Xem danh sách phim đang chiếu/sắp chiếu trên trang chủ.",
    "Xem chi tiết phim gồm thông tin mô tả, thời lượng, thể loại, poster, trailer/rating nếu có.",
    "Xem danh sách suất chiếu theo phim hoặc theo rạp.",
    "Xem bản đồ rạp, vị trí rạp và mở chỉ đường Google Maps.",
    "Đăng ký tài khoản và đăng nhập để sử dụng chức năng đặt vé.",
    "Chọn ghế trên sơ đồ ghế của một suất chiếu.",
    "Giữ ghế trong 10 phút để tránh người khác đặt trùng.",
    "Tạo booking từ các ghế đang giữ.",
    "Áp dụng mã khuyến mãi nếu mã hợp lệ.",
    "Khởi tạo thanh toán và nhận kết quả thanh toán.",
    "Xem lịch sử booking và chi tiết vé.",
    "Cập nhật thông tin hồ sơ cá nhân.",
]:
    chapters.append(bullet(item))

chapters += [
    h2("3.2. Yêu cầu chức năng phía nhân viên"),
]
for item in [
    "Đăng nhập bằng tài khoản có role STAFF.",
    "Truy cập màn hình soát vé QR.",
    "Quét mã QR từ vé điện tử của khách hàng.",
    "Gửi mã QR lên backend để kiểm tra trạng thái vé.",
    "Chuyển trạng thái vé từ ACTIVE sang USED khi check-in thành công.",
    "Không cho phép sử dụng lại vé đã check-in hoặc vé đã bị hủy.",
]:
    chapters.append(bullet(item))

chapters += [
    h2("3.3. Yêu cầu chức năng phía quản trị viên"),
]
for item in [
    "Quản lý danh sách phim: thêm, sửa, xóa, phân trang, tìm kiếm.",
    "Quản lý rạp chiếu: thêm, sửa, xóa, nhập địa chỉ và tọa độ.",
    "Quản lý suất chiếu: tạo suất chiếu theo phim/phòng, cập nhật, xóa.",
    "Quản lý người dùng: xem danh sách, tìm kiếm, khóa và mở khóa tài khoản.",
    "Theo dõi dashboard gồm doanh thu, số booking, số vé và biểu đồ doanh thu.",
    "Xem top phim theo doanh thu và hiệu suất suất chiếu.",
    "Có quyền truy cập rộng hơn thông qua role ADMIN được gán toàn bộ permission.",
]:
    chapters.append(bullet(item))

chapters += [
    h2("3.4. Yêu cầu phi chức năng"),
]
for item in [
    "Tính đúng đắn dữ liệu: một ghế trong một suất chiếu chỉ có một trạng thái hợp lệ tại một thời điểm.",
    "Tính đồng thời: thao tác giữ ghế cần khóa dữ liệu để giảm race condition.",
    "Tính bảo mật: API quan trọng phải yêu cầu JWT và permission phù hợp.",
    "Tính mở rộng: tách controller, service, repository, DTO, mapper để dễ bảo trì.",
    "Tính trải nghiệm: frontend cần phản hồi nhanh, có loading, protected route và cập nhật realtime.",
    "Tính vận hành: có logging, file log, Docker Compose cho PostgreSQL và dữ liệu mẫu để demo.",
    "Tính kiểm thử: cần có test tự động cho các luồng rủi ro cao; hiện mới có test cơ bản.",
]:
    chapters.append(bullet(item))

chapters += section("CHƯƠNG 4. THIẾT KẾ KIẾN TRÚC HỆ THỐNG", [
    "Kiến trúc tổng thể của hệ thống gồm frontend React, backend Spring Boot và cơ sở dữ liệu PostgreSQL. Frontend gửi request HTTP đến backend thông qua Vite proxy trong môi trường development. Backend xử lý request, kiểm tra xác thực/phân quyền, gọi service nghiệp vụ, truy vấn repository và trả response theo định dạng ApiResponse. Đối với trạng thái ghế, backend sử dụng WebSocket để đẩy sự kiện đến client đang xem cùng suất chiếu.",
    "Kiến trúc backend được chia thành nhiều layer. Controller chỉ nhận request, gọi service và đóng gói response. Service chứa logic nghiệp vụ, transaction, kiểm tra trạng thái và phối hợp nhiều repository. Repository truy cập dữ liệu. Entity biểu diễn bảng database. DTO biểu diễn dữ liệu vào/ra. Mapper giúp chuyển đổi entity và DTO. Exception handler chuẩn hóa lỗi. Security layer xử lý JWT và permission. WebSocket layer chịu trách nhiệm phát sự kiện realtime.",
])

chapters += [table(backend_modules, [2400, 1200, 5400])]

chapters += [
    h2("4.1. Kiến trúc backend"),
    p("Backend đặt package gốc là com.cinema.booking. Việc chia thư mục rõ ràng giúp dự án dễ đọc: controller, service, repository, entity, dto, mapper, enums, exception, security, configuration, websocket và util. Đây là cách tổ chức phổ biến trong Spring Boot, phù hợp cho một khóa luận cần thể hiện tư duy phân tầng."),
    p("Luồng xử lý một request điển hình như sau: frontend gửi request kèm token; SecurityFilterChain kiểm tra endpoint public hay cần xác thực; nếu cần xác thực, CustomJwtDecoder giải mã token và tạo Authentication; controller nhận request DTO; service xử lý nghiệp vụ trong transaction; repository truy vấn database; mapper chuyển kết quả sang response DTO; controller trả ApiResponse cho client. Nếu có lỗi, AppException và GlobalExceptionHandler trả mã lỗi thống nhất."),
    h2("4.2. Kiến trúc frontend"),
    p("Frontend cinema-client dùng React component và route layout. Các trang public/user dùng PublicLayout có navbar và footer. Các trang auth dùng AuthLayout riêng. Các trang admin/staff dùng AdminLayout dạng sidebar. AppRouter lazy-load các page để giảm tải ban đầu. ProtectedRoute kiểm tra token và permission trước khi cho vào trang cần quyền."),
]

chapters += [table(frontend_modules, [2400, 1200, 5400])]

chapters += [
    p("Axios client dùng baseURL rỗng để tận dụng Vite proxy. Interceptor request tự động thêm Bearer token từ Zustand auth store. Interceptor response xử lý HTTP 401 bằng cách logout. Cách làm này giúp từng API module không cần lặp logic token và tăng tính nhất quán trong toàn bộ frontend."),
    h2("4.3. Kiến trúc realtime"),
    p("Backend cấu hình STOMP broker với topic prefix. Frontend dùng hook useSeatWebSocket để kết nối tới /ws-native, tự reconnect sau 3 giây khi mất kết nối, subscribe /topic/seatmap/{showtimeId} và gọi callback cập nhật ghế. Đây là cách tiếp cận phù hợp cho chức năng seat map vì mỗi suất chiếu là một kênh độc lập, tránh việc client nhận quá nhiều sự kiện không liên quan."),
    h2("4.4. Kiến trúc thanh toán"),
    p("PaymentServiceImpl tạo bản ghi Payment ở trạng thái PENDING, kiểm tra booking thuộc về user hiện tại và kiểm tra amount khớp với totalPrice. Nếu method là VNPAY, service sinh URL thanh toán có chữ ký HMAC-SHA512. Callback VNPay được xác thực chữ ký, đọc transaction reference, response code và secureToken, sau đó cập nhật Payment và gọi BookingService để chuyển booking sang success hoặc failed."),
    p("Việc tách PaymentService và BookingService giúp luồng thanh toán rõ trách nhiệm: PaymentService làm việc với gateway và bản ghi payment, BookingService xử lý trạng thái booking, ghế, ticket, email và websocket. Đây là điểm thiết kế tốt vì payment gateway có thể mở rộng sang MoMo/CASH mà không làm rối logic booking chính."),
]

chapters += section("CHƯƠNG 5. THIẾT KẾ CƠ SỞ DỮ LIỆU", [
    "Cơ sở dữ liệu được thiết kế theo hướng quan hệ, phù hợp với PostgreSQL. Các bảng chính phản ánh các thực thể nghiệp vụ trong hệ thống rạp phim: users, roles, permissions, movies, cinemas, rooms, seats, showtimes, seat_status, bookings, booking_details, tickets, payments, promotions và invalidated_token. Các entity đều dùng UUID làm khóa chính, giúp giảm phụ thuộc vào số tự tăng và thuận tiện khi mở rộng hoặc đồng bộ dữ liệu.",
    "Hệ thống sử dụng BaseEntity cho các trường createdAt và updatedAt, được quản lý bởi AuditingEntityListener. Một số entity có isDeleted để hỗ trợ xóa mềm. Việc dùng xóa mềm phù hợp với nghiệp vụ rạp phim vì nhiều dữ liệu như phim, rạp, ghế, suất chiếu hoặc booking có thể cần giữ lại cho báo cáo, lịch sử và đối soát.",
])

chapters += [table(entity_rows, [1800, 4000, 3200])]

chapters += [
    h2("5.1. Thiết kế SeatStatus"),
    p("SeatStatus là bảng đặc biệt quan trọng trong hệ thống. Một ghế vật lý thuộc một phòng có thể được dùng cho nhiều suất chiếu khác nhau. Do đó, trạng thái ghế không thể lưu trực tiếp trong bảng seats, mà cần lưu theo cặp showtime-seat. Mỗi dòng SeatStatus biểu diễn trạng thái của một ghế trong một suất chiếu cụ thể: AVAILABLE, HOLD hoặc BOOKED."),
    p("Thiết kế này giúp cùng một ghế A1 ở phòng 1 có thể AVAILABLE cho suất chiếu 09:00, BOOKED cho suất chiếu 12:30 và HOLD cho suất chiếu 19:30. Unique constraint trên showtime_id và seat_id đảm bảo không có hai trạng thái khác nhau cho cùng một ghế trong cùng một suất chiếu."),
    h2("5.2. Thiết kế Booking và Ticket"),
    p("Booking lưu thông tin đơn đặt vé ở mức tổng: người dùng, suất chiếu, khuyến mãi, tổng tiền, giảm giá, trạng thái và secureToken. BookingDetail lưu từng ghế được đặt và giá tại thời điểm đặt. Cách tách Booking và BookingDetail giúp một đơn có nhiều vé/ghế, đồng thời vẫn giữ được giá ghế tại thời điểm mua dù sau này giá suất chiếu thay đổi."),
    p("Ticket được sinh sau khi thanh toán thành công, liên kết với BookingDetail. Mỗi ticket có qrCode unique và trạng thái ACTIVE/USED/CANCELLED. Khi nhân viên check-in, hệ thống tìm ticket theo qrCode, kiểm tra trạng thái và cập nhật checkInTime. Thiết kế này phù hợp với vé điện tử vì mỗi ghế sau thanh toán tương ứng một vé riêng."),
    h2("5.3. Thiết kế Promotion"),
    p("Promotion hỗ trợ hai kiểu giảm giá: PERCENT và FIXED. Với PERCENT, hệ thống tính discount bằng orderValue nhân discountValue chia 100, có thể giới hạn bởi maxDiscountAmount. Với FIXED, hệ thống giảm một số tiền cố định nhưng không vượt quá tổng đơn. Các trường startDate, endDate, usageLimit, usedCount, minOrderValue và isActive giúp kiểm soát tính hợp lệ của mã khuyến mãi."),
    h2("5.4. Dữ liệu mẫu"),
    p("File mock-data.sql tạo dữ liệu mẫu nghiệp vụ gồm user test, phim, rạp, phòng, ghế, khuyến mãi, suất chiếu và seat_status. Dữ liệu RBAC cốt lõi không seed bằng file SQL mà được ApplicationInitConfig quản lý khi backend khởi động. Cách tách này giúp quyền/role luôn đồng bộ với enum PermissionName trong code, trong khi dữ liệu demo có thể reset độc lập."),
    p("Dữ liệu mẫu hiện mô phỏng nhiều rạp, mỗi rạp có hai phòng, mỗi phòng 8 hàng x 12 ghế, phân loại ghế thường, VIP và couple. Các suất chiếu được sinh trong nhiều ngày tới với nhiều khung giờ và giá cơ sở khác nhau. Điều này rất hữu ích cho demo vì frontend có đủ dữ liệu để hiển thị phim, rạp, suất chiếu và sơ đồ ghế."),
]

chapters += section("CHƯƠNG 6. PHÂN TÍCH BACKEND CINEMA-BOOKING-SYSTEM", [
    "Backend cinema-booking-system là thành phần có khối lượng xử lý lớn nhất. Dự án sử dụng Maven, Java 21 và Spring Boot 3.5.14. Các dependency đáng chú ý gồm spring-boot-starter-web, data-jpa, security, oauth2-resource-server, validation, websocket, mail, thymeleaf, PostgreSQL driver, SpringDoc OpenAPI, Lombok và nimbus-jose-jwt. Cấu hình application.yaml đọc biến môi trường từ .env thông qua spring-dotenv và cấu hình datasource, JPA, mail, VNPay, JWT, logging.",
    "Backend hiện có 12 controller, 12 service interface, 11 service implementation, 16 entity, 12 enum, 14 repository, 12 mapper, 23 request DTO và 24 response DTO. Cấu trúc này cho thấy dự án đã phát triển vượt mức prototype đơn giản và đã có tổ chức tương đối đầy đủ theo domain nghiệp vụ.",
])

chapters += [table(api_rows, [1800, 3600, 3600])]

chapters += [
    h2("6.1. AuthenticationController và AuthenticationService"),
    p("AuthenticationController cung cấp các endpoint /auth/token, /auth/introspect, /auth/refresh và /auth/logout. Đây là nhóm endpoint public cho phép người dùng đăng nhập, kiểm tra token, làm mới token và đăng xuất. Backend dùng JWT signer key, access-token-valid-duration và refresh-token-valid-duration trong application.yaml. Khi logout, token có thể được đưa vào InvalidatedToken để tránh sử dụng lại trước khi hết hạn."),
    p("Ở phía frontend, LoginPage gọi authApi.login để lấy token, sau đó gọi API profile /api/v1/users/me để lấy thông tin người dùng và permission. Zustand authStore lưu token, user và permissions vào localStorage. ProtectedRoute dựa vào thông tin này để chặn truy cập trang yêu cầu đăng nhập hoặc permission cụ thể."),
    h2("6.2. UserController và quản lý người dùng"),
    p("UserController có endpoint đăng ký public /api/v1/users/register và các endpoint quản trị như tạo user, xem danh sách, xem chi tiết, cập nhật, xóa, khóa/mở khóa tài khoản. Ngoài ra còn có /api/v1/users/me và PATCH /api/v1/users/me để người dùng tự xem/cập nhật hồ sơ. UserServiceImpl kiểm tra username/email trùng, mã hóa password bằng BCrypt và gán role USER mặc định khi đăng ký."),
    p("Chức năng khóa tài khoản có kiểm tra không cho người dùng tự khóa chính mình. Đây là một chi tiết nghiệp vụ quan trọng, tránh trường hợp admin đang đăng nhập vô tình tự chặn tài khoản và làm mất khả năng vận hành. Frontend AdminUserPage đã có bảng danh sách user, tìm kiếm và nút khóa/mở khóa."),
    h2("6.3. Movie, Cinema, Room, Seat và Showtime"),
    p("Các module này tạo nền tảng dữ liệu cho nghiệp vụ đặt vé. Movie quản lý thông tin phim. Cinema quản lý rạp và tọa độ bản đồ. Room quản lý phòng theo rạp. Seat quản lý ghế trong từng phòng, hỗ trợ sinh ghế hàng loạt. Showtime liên kết phim với phòng trong một khoảng thời gian, có giá cơ sở và trạng thái. Khi tạo suất chiếu, service có logic tạo seat_status cho các ghế trong phòng."),
    p("ShowtimeServiceImpl cần đảm bảo thời gian bắt đầu/kết thúc hợp lệ, kiểm tra trùng lịch chiếu trong cùng phòng và tạo dữ liệu trạng thái ghế. Nếu thiếu seat_status, BookingServiceImpl cũng có hàm seedMissingSeatStatuses để tự bù khi lấy sơ đồ ghế. Đây là cơ chế phòng vệ giúp dữ liệu không bị thiếu khi có thay đổi hoặc seed chưa đủ."),
    h2("6.4. BookingServiceImpl"),
    p("BookingServiceImpl là trung tâm của nghiệp vụ đặt vé. Luồng giữ ghế bắt đầu bằng việc lấy current user từ SecurityUtils, kiểm tra showtime còn bookable, load SeatStatus bằng PESSIMISTIC_WRITE lock, xác nhận số ghế và trạng thái AVAILABLE, đặt status HOLD, gắn holdBy và holdUntil, tính estimatedTotalPrice, lưu dữ liệu và publish WebSocket event HOLD."),
    p("Luồng tạo booking kiểm tra lại các ghế vẫn đang HOLD bởi chính user hiện tại, chưa hết hạn, không bị người khác giữ. Sau đó hệ thống tính totalBeforeDiscount dựa trên basePrice của showtime và priceMultiplier của từng ghế, áp dụng promotion nếu có, tạo Booking ở trạng thái PENDING, tạo BookingDetail cho từng ghế và trả BookingResponse. Việc tách hold và create booking giúp người dùng có thời gian xác nhận ghế trước khi tạo đơn."),
    p("Khi thanh toán thành công, handlePaymentSuccess tìm booking theo secureToken, kiểm tra booking còn PENDING, đổi status SUCCESS, bulk-update seat_status thành BOOKED, publish WebSocket event BOOKED, sinh Ticket cho từng BookingDetail, tăng usedCount của promotion nếu có, lưu booking và gửi email vé bất đồng bộ. Khi thanh toán thất bại, handlePaymentFailure đổi booking thành FAILED, nhả ghế về AVAILABLE và publish event AVAILABLE."),
    p("Khi hủy booking, hệ thống chỉ cho hủy booking PENDING. User thường chỉ được hủy booking của mình, còn người có BOOKING_CANCEL_ALL có thể hủy booking của người khác. Sau hủy, ghế được trả về AVAILABLE và frontend nhận WebSocket event. Các trạng thái BookingStatus gồm PENDING, SUCCESS, FAILED và CANCELLED."),
    h2("6.5. PaymentServiceImpl"),
    p("PaymentServiceImpl đảm nhận việc khởi tạo thanh toán và xử lý callback. Khi initiatePayment, service kiểm tra booking tồn tại, thuộc về user hiện tại, còn PENDING và amount khớp totalPrice. Sau đó tạo Payment PENDING với transactionNo. Nếu phương thức là VNPAY, service gọi generateVNPayUrl để tạo URL sandbox có tham số vnp_TxnRef, vnp_OrderInfo, vnp_Amount, vnp_IpAddr, vnp_CreateDate, vnp_ExpireDate và vnp_SecureHash."),
    p("Callback VNPay kiểm tra chữ ký bằng hash secret. Nếu responseCode là 00, payment chuyển SUCCESS, paymentTime được ghi nhận và BookingService.handlePaymentSuccess được gọi. Nếu thất bại, payment chuyển FAILED và BookingService.handlePaymentFailure được gọi. Đây là cách đồng bộ payment và booking khá rõ ràng, nhưng cần bổ sung test cho các tình huống callback lặp, sai chữ ký, amount sai hoặc secureToken không hợp lệ."),
    h2("6.6. EmailService và vé điện tử"),
    p("Sau thanh toán thành công, emailService.sendTicketEmail(saved.getId()) được gọi bất đồng bộ. Việc truyền UUID thay vì entity giúp tránh LazyInitializationException khi @Async chạy ở thread khác. Template ticket-email.html nằm trong resources/templates. Email có thể được test bằng Mailtrap trong môi trường phát triển, đúng như hướng dẫn README backend."),
    h2("6.7. AnalyticsService"),
    p("AnalyticsController cung cấp các API summary, revenue daily/monthly, top movie revenue và showtime stats. PaymentRepository và BookingRepository có các query phục vụ thống kê như tổng doanh thu, doanh thu theo ngày/tháng, số booking, số vé và top phim. Frontend AdminDashboardPage dùng Recharts để hiển thị dữ liệu dashboard."),
    h2("6.8. Scheduler"),
    p("Dự án có TokenCleanupTask chạy cron 02:00 hằng ngày để dọn token đã hết hạn và HoldExpireScheduler để xử lý ghế HOLD quá hạn. Scheduler giữ cho dữ liệu vận hành sạch hơn và giảm rủi ro ghế bị giữ mãi nếu người dùng rời khỏi trang hoặc không thanh toán. Đây là phần quan trọng đối với hệ thống đặt vé thực tế."),
]

chapters += section("CHƯƠNG 7. PHÂN TÍCH FRONTEND CINEMA-CLIENT", [
    "Frontend cinema-client là ứng dụng React + TypeScript + Vite. package.json cho thấy dự án dùng nhiều thư viện phù hợp với ứng dụng đặt vé: React Router cho điều hướng, React Query cho dữ liệu server, Zustand cho state local, Axios cho HTTP, STOMP/WebSocket cho realtime, Leaflet cho bản đồ, Recharts cho biểu đồ, html5-qrcode cho quét vé, react-hook-form và zod cho form validation, lucide-react cho icon.",
    "Ứng dụng được chia theo nhóm trang public, user, admin và staff. AppRouter lazy-load từng page, giúp giảm bundle tải ban đầu. Layout được tách thành PublicLayout, AuthLayout và AdminLayout. Navbar hiển thị các mục khác nhau theo trạng thái đăng nhập và quyền của người dùng. AdminLayout lọc nav item theo permission, ví dụ DASHBOARD_VIEW, MOVIE_CREATE, CINEMA_CREATE, SHOWTIME_CREATE và USER_VIEW.",
])

chapters += [
    h2("7.1. Các route chính"),
    table([
        ["Route", "Trang", "Đối tượng"],
        ["/", "HomePage", "Public"],
        ["/movies/:id", "MovieDetailPage", "Public"],
        ["/cinemas", "CinemaMapPage", "Public"],
        ["/login", "LoginPage", "Public/auth"],
        ["/register", "RegisterPage", "Public/auth"],
        ["/seat-selection/:showtimeId", "SeatSelectionPage", "User đã đăng nhập"],
        ["/checkout/:bookingId", "CheckoutPage", "User đã đăng nhập"],
        ["/payment/result", "PaymentResultPage", "User đã đăng nhập"],
        ["/my/bookings", "MyBookingsPage", "User đã đăng nhập"],
        ["/my/bookings/:bookingId", "TicketDetailPage", "User đã đăng nhập"],
        ["/profile", "ProfilePage", "User đã đăng nhập"],
        ["/staff/scanner", "StaffTicketScannerPage", "Permission TICKET_CHECKIN"],
        ["/admin/dashboard", "AdminDashboardPage", "Permission DASHBOARD_VIEW"],
        ["/admin/movies", "AdminMoviePage", "Permission MOVIE_CREATE"],
        ["/admin/cinemas", "AdminCinemaPage", "Permission CINEMA_CREATE"],
        ["/admin/showtimes", "AdminShowtimePage", "Permission SHOWTIME_CREATE"],
        ["/admin/users", "AdminUserPage", "Permission USER_VIEW"],
    ], [2500, 3000, 3500]),
    h2("7.2. API client"),
    p("Frontend tách API theo domain: authApi, analyticsApi, bookingApi, cinemaApi, movieApi, paymentApi, ticketApi và userApi. Cách tách này giúp page component chỉ gọi đúng module cần dùng, giảm việc hardcode endpoint rải rác trong giao diện. bookingApi có các hàm getSeatMap, holdSeats, createBooking, getMyBookings, getBookingById và cancelBooking. paymentApi có initiatePayment và getMyPayments. ticketApi có getMyTickets, checkIn và getAllTickets."),
    p("axiosClient được cấu hình baseURL rỗng, nhờ Vite proxy chuyển /api và /auth sang backend. Điều này giúp frontend khi chạy local không cần gọi trực tiếp http://localhost:8080 trong code. Với WebSocket, Vite proxy cũng cấu hình /ws và /ws-native có ws: true để hỗ trợ STOMP/SockJS/native WebSocket."),
    h2("7.3. Quản lý xác thực bằng Zustand"),
    p("authStore lưu token, user và permissions vào localStorage. Khi login thành công, store ghi dữ liệu này và cập nhật state. Khi logout hoặc gặp lỗi 401, store xóa dữ liệu và chuyển người dùng về /login. Hàm hasPermission kiểm tra permission trong mảng permissions. Cách làm này đơn giản, phù hợp với phạm vi khóa luận, nhưng về production có thể cân nhắc lưu token trong httpOnly cookie để giảm rủi ro XSS."),
    h2("7.4. Trang chọn ghế và WebSocket"),
    p("SeatSelectionPage là trang quan trọng nhất của frontend user. Trang này tải seat map từ backend, cho người dùng chọn ghế, gọi hold API, gọi create booking và chuyển sang checkout. Hook useSeatWebSocket subscribe trạng thái ghế theo showtimeId; khi có event, trang cập nhật ghế tương ứng. Trạng thái HOLD có heldByUserId giúp UI phân biệt ghế chính mình đang giữ với ghế người khác giữ."),
    h2("7.5. Trang checkout và payment result"),
    p("CheckoutPage lấy booking theo bookingId, hiển thị thông tin đơn và gọi paymentApi.initiatePayment để nhận URL thanh toán. PaymentResultPage xử lý kết quả sau thanh toán. Ở hiện trạng, backend callback VNPay redirect về đường dẫn payment-success/payment-failed phía backend; frontend đã có route /payment/result, nên cần tiếp tục đồng bộ redirect URL frontend/backend để trải nghiệm trọn vẹn hơn trong bản production."),
    h2("7.6. Trang bản đồ rạp"),
    p("CinemaMapPage dùng React Leaflet để hiển thị các rạp có latitude/longitude. API /api/v1/cinemas/map trả danh sách rạp trên bản đồ, /api/v1/cinemas/nearest có thể tìm rạp gần nhất dựa trên tọa độ người dùng. Đây là điểm cộng của đề tài vì mở rộng khỏi CRUD đơn thuần và đem lại trải nghiệm thực tế cho khách hàng."),
    h2("7.7. Trang staff scanner"),
    p("StaffTicketScannerPage dùng html5-qrcode để quét mã QR từ vé. Sau khi quét, frontend gọi /api/v1/tickets/check-in. Backend kiểm tra trạng thái vé và cập nhật USED. Chức năng này hoàn thiện một vòng đời vé: từ chọn ghế, thanh toán, sinh QR, hiển thị vé đến soát vé tại rạp."),
    h2("7.8. Trang admin"),
    p("AdminDashboardPage gọi analyticsApi để lấy summary, doanh thu ngày/tháng, top phim và thống kê suất chiếu. AdminMoviePage, AdminCinemaPage, AdminShowtimePage và AdminUserPage dùng React Query để fetch dữ liệu, mutation để tạo/sửa/xóa/khóa/mở khóa và invalidate query sau khi thao tác thành công. Đây là cách tiếp cận hiện đại, giúp UI tự cập nhật sau thao tác mà không cần reload toàn trang."),
]

chapters += section("CHƯƠNG 8. LUỒNG NGHIỆP VỤ CHI TIẾT", [
    "Chương này trình bày các luồng nghiệp vụ quan trọng nhất của hệ thống. Các luồng được mô tả theo đúng cách backend và frontend hiện đang tổ chức mã nguồn. Việc phân tích luồng giúp chứng minh hệ thống đã xử lý không chỉ dữ liệu tĩnh mà còn các trạng thái thay đổi theo thời gian và theo giao dịch.",
])

flow_sections = [
    ("8.1. Luồng đăng ký và đăng nhập", [
        "Người dùng mới truy cập /register, nhập username, password và thông tin cá nhân. Frontend dùng react-hook-form và zod để validation cơ bản trước khi gửi request. Backend UserController.register nhận UserCreationRequest, UserServiceImpl kiểm tra username/email trùng, mã hóa password bằng BCrypt và gán role USER.",
        "Khi đăng nhập, LoginPage gọi POST /auth/token. Nếu thông tin hợp lệ, backend trả AuthenticationResponse chứa token. Frontend tiếp tục gọi /api/v1/users/me để lấy thông tin user và permission, sau đó lưu vào authStore. Các request tiếp theo tự động có Authorization header. Nếu token hết hạn hoặc lỗi 401, interceptor logout và chuyển về login.",
    ]),
    ("8.2. Luồng xem phim và chọn suất chiếu", [
        "Trang chủ gọi movieApi để lấy danh sách phim. Người dùng chọn một phim và vào MovieDetailPage. Trang chi tiết phim gọi getMovieById và getShowtimesByMovie. Người dùng chọn suất chiếu phù hợp, sau đó được chuyển đến /seat-selection/{showtimeId}.",
        "Các endpoint public GET /api/v1/movies/**, /api/v1/showtimes/** và /api/v1/cinemas/map được SecurityConfig permitAll, vì người chưa đăng nhập vẫn cần xem phim, lịch chiếu và rạp. Tuy nhiên, khi chọn ghế/giữ ghế/tạo booking, người dùng phải đăng nhập vì các endpoint booking yêu cầu permission BOOKING_CREATE.",
    ]),
    ("8.3. Luồng giữ ghế", [
        "SeatSelectionPage gọi getSeatMap để lấy trạng thái ghế hiện tại. Khi người dùng chọn ghế và xác nhận giữ, frontend gửi POST /api/v1/bookings/hold với showtimeId và danh sách seatIds. Backend dùng PESSIMISTIC_WRITE để khóa các dòng SeatStatus tương ứng.",
        "Nếu có ghế không tồn tại hoặc không AVAILABLE, backend trả lỗi. Nếu hợp lệ, backend đặt status HOLD, holdBy là user hiện tại, holdUntil = now + 10 phút, tính estimatedTotalPrice và publish WebSocket event HOLD. Các client khác đang xem cùng showtime nhận event và cập nhật màu ghế.",
    ]),
    ("8.4. Luồng tạo booking", [
        "Sau khi ghế đã được HOLD, frontend gọi POST /api/v1/bookings. Backend kiểm tra lại trạng thái từng ghế: phải là HOLD, chưa hết hạn và holdBy đúng user. Đây là bước bảo vệ quan trọng vì giữa lúc hold và tạo booking có thể có scheduler nhả ghế hoặc dữ liệu thay đổi.",
        "Backend tính tổng tiền dựa trên basePrice và priceMultiplier. Nếu có promotionCode, backend kiểm tra mã còn hoạt động, trong thời hạn, chưa vượt usageLimit và đạt minOrderValue. Booking được tạo ở trạng thái PENDING với secureToken. BookingDetail được tạo cho từng ghế.",
    ]),
    ("8.5. Luồng thanh toán thành công", [
        "Frontend ở CheckoutPage gọi payment initiate với bookingId, method và amount. Backend tạo Payment PENDING và trả URL VNPay hoặc mock URL. Khi gateway callback về backend, PaymentService xác thực chữ ký và responseCode.",
        "Nếu thanh toán thành công, Payment chuyển SUCCESS, Booking chuyển SUCCESS, seat_status chuyển BOOKED, ticket được sinh cho từng ghế, QR code được tạo, promotion usedCount tăng, email vé được gửi bất đồng bộ và WebSocket publish BOOKED. Người dùng sau đó có thể xem vé trong /my/bookings hoặc /api/v1/tickets/my.",
    ]),
    ("8.6. Luồng thanh toán thất bại hoặc hủy booking", [
        "Nếu callback báo thất bại, Payment chuyển FAILED, Booking chuyển FAILED và ghế được trả về AVAILABLE. Nếu người dùng hủy booking khi còn PENDING, Booking chuyển CANCELLED và ghế cũng được trả về AVAILABLE. Cả hai trường hợp đều publish WebSocket event AVAILABLE để frontend cập nhật.",
        "Hệ thống chỉ cho hủy booking PENDING. Booking đã SUCCESS không thể hủy bằng luồng hiện tại, vì lúc đó đã phát vé và ghế đã BOOKED. Nếu muốn hỗ trợ hoàn tiền/hủy vé sau thanh toán, cần bổ sung quy trình refund, policy thời gian và trạng thái ticket/payment mở rộng.",
    ]),
    ("8.7. Luồng check-in vé", [
        "Sau thanh toán thành công, mỗi ticket có QR code unique. Nhân viên đăng nhập vào /staff/scanner, camera quét QR và gửi qrCode đến backend. Backend tìm Ticket theo qrCode, nếu không thấy trả TICKET_NOT_FOUND; nếu đã USED trả TICKET_ALREADY_USED; nếu CANCELLED trả TICKET_CANCELLED.",
        "Nếu vé hợp lệ, backend set status USED và checkInTime = now. Điều này giúp rạp kiểm soát việc vào phòng chiếu, chống dùng lại vé và có thể thống kê lượng khách đã check-in thực tế.",
    ]),
]

for title, paras in flow_sections:
    chapters.append(h2(title))
    for para in paras:
        chapters.append(p(para))

chapters += [
    h2("8.8. Mã giả luồng giữ ghế"),
    code("User chọn ghế -> POST /api/v1/bookings/hold"),
    code("Backend: load SeatStatus bằng PESSIMISTIC_WRITE"),
    code("Nếu tất cả AVAILABLE: set HOLD, holdBy, holdUntil, saveAll"),
    code("Backend: publish /topic/seatmap/{showtimeId} với status HOLD"),
    code("Frontend: nhận event, cập nhật seat map realtime"),
    h2("8.9. Mã giả luồng thanh toán thành công"),
    code("VNPay callback -> verify signature"),
    code("Payment PENDING -> SUCCESS"),
    code("Booking PENDING -> SUCCESS"),
    code("SeatStatus HOLD -> BOOKED"),
    code("Generate Ticket + QR for each BookingDetail"),
    code("Send email async, publish BOOKED event"),
]

chapters += section("CHƯƠNG 9. BẢO MẬT, PHÂN QUYỀN VÀ AN TOÀN DỮ LIỆU", [
    "Bảo mật của hệ thống gồm nhiều lớp: xác thực JWT, phân quyền method-level bằng @PreAuthorize, mã hóa mật khẩu bằng BCrypt, kiểm tra quyền sở hữu booking/payment, kiểm tra trạng thái nghiệp vụ trước khi cập nhật, xác thực chữ ký VNPay và xử lý token logout bằng InvalidatedToken. Các endpoint public được giới hạn rõ ràng trong SecurityConfig.",
    "SecurityConfig permit các endpoint auth, register, movie/showtime public, seat map public, cinema map/nearest, Swagger và WebSocket. Các endpoint còn lại yêu cầu authenticated. Sau đó, từng controller tiếp tục dùng @PreAuthorize theo permission. Thiết kế này có hai tầng: tầng route yêu cầu đăng nhập và tầng method yêu cầu quyền cụ thể.",
])

chapters += [
    h2("9.1. Phân quyền role"),
    table([
        ["Role", "Quyền chính", "Mục đích"],
        ["USER", "MOVIE_VIEW, CINEMA_VIEW, SHOWTIME_VIEW, SEAT_VIEW, BOOKING_CREATE, BOOKING_VIEW_OWN, PAYMENT_CREATE, TICKET_VIEW_OWN, PROFILE_UPDATE", "Khách hàng đặt vé và quản lý thông tin cá nhân."],
        ["STAFF", "MOVIE_VIEW, ROOM_VIEW, SEAT_UPDATE, SHOWTIME_CREATE/UPDATE, BOOKING_VIEW_ALL, PAYMENT_VIEW_ALL, TICKET_VIEW_ALL, TICKET_CHECKIN, ANALYTICS_VIEW", "Nhân viên vận hành rạp và soát vé."],
        ["ADMIN", "Toàn bộ permissions", "Quản trị toàn hệ thống."],
    ], [1500, 5200, 2300]),
    h2("9.2. Chống đặt trùng ghế"),
    p("Chống đặt trùng ghế là yêu cầu an toàn dữ liệu quan trọng nhất. Hệ thống dùng PESSIMISTIC_WRITE khi lấy SeatStatus trong holdSeats và createBooking. Khi một transaction đang kiểm tra/cập nhật ghế, transaction khác phải chờ, từ đó giảm nguy cơ hai user cùng chuyển một ghế AVAILABLE sang HOLD. Unique constraint theo showtime-seat đảm bảo không có hai dòng trạng thái trùng nhau."),
    p("Ngoài lock, hệ thống còn kiểm tra nhiều điều kiện nghiệp vụ: ghế phải AVAILABLE khi hold, phải HOLD khi create booking, phải do chính user hiện tại giữ, holdUntil chưa hết hạn, booking phải PENDING khi thanh toán/hủy. Đây là các guard condition giúp state transition rõ ràng và tránh cập nhật sai trạng thái."),
    h2("9.3. Bảo mật thanh toán"),
    p("VNPay callback được kiểm tra chữ ký HMAC-SHA512 bằng hash secret. Nếu chữ ký không hợp lệ, service redirect về payment-failed với reason invalid-signature và không cập nhật booking. Payment transactionNo được dùng để tìm bản ghi Payment. secureToken trong OrderInfo giúp callback biết booking cần xử lý. Cơ chế này phù hợp cho sandbox, nhưng production nên tránh đặt token nhạy cảm trong order info nếu gateway/log có thể lộ, hoặc cần ký/đối chiếu thêm amount và order metadata."),
    h2("9.4. Các điểm cần tăng cường"),
]
for item in [
    "Không lưu access token trong localStorage ở production nếu có yêu cầu bảo mật cao; cân nhắc httpOnly secure cookie.",
    "Bổ sung rate limit cho đăng nhập, giữ ghế và payment initiate.",
    "Bổ sung audit log cho admin action, check-in và thay đổi trạng thái booking.",
    "Bổ sung kiểm tra amount trong callback gateway, không chỉ khi initiate.",
    "Bổ sung idempotency cho payment callback để callback lặp không gây lỗi vận hành.",
    "Ẩn hoặc giảm logging SQL/parameter ở production để tránh lộ dữ liệu.",
    "Bổ sung CORS policy cụ thể khi deploy tách domain frontend/backend.",
]:
    chapters.append(bullet(item))

chapters += section("CHƯƠNG 10. KIỂM THỬ, ĐÁNH GIÁ HIỆN TRẠNG VÀ TIẾN ĐỘ", [
    "Tại thời điểm lập báo cáo, backend đã được chạy kiểm thử bằng Maven. Do môi trường ban đầu bị hạn chế truy cập mạng nên mvn test không tải được parent/dependency từ Maven Central; sau khi cho phép Maven truy cập mạng, lệnh mvn test chạy thành công. Kết quả: Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, BUILD SUCCESS. Test hiện có là contextLoads, tức mới xác nhận Spring context khởi động được.",
    "Kết quả này cho thấy dự án có thể compile và khởi động context trong môi trường có dependency và database phù hợp. Tuy nhiên, số lượng test tự động hiện vẫn ít so với độ phức tạp nghiệp vụ. Các luồng booking/payment/security cần được bổ sung unit test và integration test để đảm bảo chất lượng trước khi nộp bản cuối hoặc triển khai demo cho nhiều người dùng.",
])

chapters += [table(test_rows, [2200, 3000, 3800])]
chapters += [h2("10.1. Bảng tiến độ hiện tại"), table(progress_rows, [2400, 2300, 4300])]

chapters += [
    h2("10.2. Đánh giá điểm mạnh"),
]
for item in [
    "Kiến trúc backend rõ ràng, tách layer tốt, dễ giải thích trong khóa luận.",
    "Luồng đặt vé đã xử lý được điểm khó là giữ ghế theo thời gian và chống đặt trùng bằng lock.",
    "Có WebSocket realtime, đây là yếu tố thực tế và nâng chất lượng đề tài.",
    "Có tích hợp VNPay sandbox/callback, không chỉ mock nội bộ.",
    "Có sinh vé QR và màn staff scanner, hoàn thiện vòng đời vé.",
    "Có RBAC chi tiết theo permission thay vì chỉ role đơn giản.",
    "Frontend đã có đầy đủ nhiều nhóm màn hình cho public, user, admin và staff.",
    "Có dữ liệu mẫu phong phú để demo phim, rạp, phòng, ghế, suất chiếu và promotion.",
]:
    chapters.append(bullet(item))

chapters += [
    h2("10.3. Hạn chế hiện tại"),
]
for item in [
    "Kiểm thử tự động chưa đủ sâu; mới có contextLoads.",
    "Frontend chưa có test runner và chưa có E2E test.",
    "Một số backend module như promotion/room/seat đã có API nhưng frontend admin chưa có đầy đủ màn riêng tương ứng.",
    "Redirect thanh toán VNPay cần đồng bộ tốt hơn với frontend /payment/result.",
    "Chưa có CI/CD, Dockerfile production cho backend/frontend hoặc cấu hình reverse proxy.",
    "Chưa có tài liệu API nghiệp vụ dạng postman collection hoặc sequence diagram chính thức.",
    "Chưa thấy xử lý refresh token tự động ở Axios khi access token hết hạn; hiện 401 sẽ logout.",
    "Chưa có kiểm thử tải cho tình huống nhiều người cùng giữ ghế.",
]:
    chapters.append(bullet(item))

chapters += [
    h2("10.4. Đề xuất bộ test cần bổ sung"),
]
for item in [
    "Test BookingService.holdSeats: giữ thành công, ghế không tồn tại, ghế đã booked, ghế đã hold bởi người khác.",
    "Test BookingService.createBooking: hold hết hạn, hold sai user, promotion hợp lệ/không hợp lệ.",
    "Test PaymentService.handleVNPayCallback: chữ ký sai, response 00, response fail, callback lặp.",
    "Test Ticket check-in: QR không tồn tại, vé active, vé used, vé cancelled.",
    "Test Security: user thường không vào admin endpoint, staff chỉ vào scanner/dashboard, admin có toàn quyền.",
    "Test frontend: login form validation, protected route, seat selection update, checkout button state.",
    "E2E: đăng nhập user1, chọn phim, giữ ghế, tạo booking, thanh toán mock, xem vé, đăng nhập staff và check-in QR.",
]:
    chapters.append(bullet(item))

chapters += section("CHƯƠNG 11. HƯỚNG PHÁT TRIỂN VÀ KẾT LUẬN", [
    "Dự án cinema-booking-system và cinema-client hiện đã có nền tảng tốt cho một hệ thống đặt vé xem phim trực tuyến. Các luồng cốt lõi đã được hiện thực ở mức có thể demo: khách hàng xem phim, chọn suất, giữ ghế, tạo booking, thanh toán, nhận vé; nhân viên quét QR; quản trị viên quản lý dữ liệu chính và xem dashboard. Backend có kiến trúc tương đối hoàn chỉnh, frontend có nhiều màn hình thực tế và dữ liệu mẫu đủ phong phú.",
    "Để trở thành sản phẩm hoàn chỉnh hơn, dự án cần tiếp tục tập trung vào chất lượng kiểm thử, hoàn thiện trải nghiệm thanh toán, bổ sung màn quản trị còn thiếu, chuẩn hóa triển khai production, tăng cường bảo mật và đo lường hiệu năng. Đây là các hạng mục phù hợp để trình bày trong phần hướng phát triển của khóa luận và cũng là cơ sở cho các lần cải tiến tiếp theo.",
])

chapters += [
    h2("11.1. Hướng phát triển ngắn hạn"),
]
for item in [
    "Hoàn thiện màn quản lý promotion, room và seat ở frontend admin.",
    "Đồng bộ returnUrl VNPay về frontend /payment/result và hiển thị trạng thái rõ ràng.",
    "Bổ sung unit/integration test cho booking, payment, security và ticket.",
    "Tạo Postman collection hoặc Swagger hướng dẫn demo từng role.",
    "Thêm ảnh chụp màn hình giao diện vào báo cáo bản nộp cuối.",
    "Giảm logging SQL ở môi trường demo nếu log quá dài hoặc chứa thông tin nhạy cảm.",
    "Hoàn thiện xử lý loading/error/empty state trên tất cả page.",
]:
    chapters.append(bullet(item))

chapters += [
    h2("11.2. Hướng phát triển dài hạn"),
]
for item in [
    "Triển khai container backend/frontend/database bằng Docker Compose production hoặc Kubernetes nhỏ.",
    "Bổ sung CI/CD chạy build, test và lint tự động khi push code.",
    "Tích hợp payment gateway production và quy trình refund/hủy vé sau thanh toán.",
    "Bổ sung hệ thống thông báo qua email/SMS/push notification.",
    "Tối ưu performance với cache phim/rạp/suất chiếu và index database.",
    "Bổ sung seat map designer để admin cấu hình sơ đồ phòng chiếu trực quan.",
    "Bổ sung báo cáo nâng cao: tỷ lệ lấp đầy phòng, doanh thu theo rạp, hiệu quả khuyến mãi, giờ cao điểm.",
    "Tăng cường bảo mật bằng rate limiting, audit log, refresh token rotation và cookie httpOnly.",
]:
    chapters.append(bullet(item))

chapters += [
    h2("11.3. Kết luận"),
    p("Qua quá trình khảo sát mã nguồn và tổng hợp báo cáo, có thể kết luận rằng đề tài đã triển khai được nhiều thành phần quan trọng của một hệ thống đặt vé xem phim trực tuyến. Backend có nghiệp vụ đặt vé đủ sâu, có cơ chế chống đặt trùng ghế, có thanh toán, vé QR, email, WebSocket và dashboard. Frontend có giao diện cho nhiều nhóm người dùng, gọi API thật, quản lý token/permission và xử lý các luồng chính."),
    p("Mức độ hoàn thiện hiện tại phù hợp để trình bày như một khóa luận đang ở giai đoạn hoàn thiện chức năng cốt lõi và bước sang giai đoạn củng cố chất lượng. Báo cáo này đã cố ý ghi rõ các phần còn thiếu, đặc biệt là test tự động, production deployment và một số màn quản trị phụ, để phản ánh trung thực tiến độ dự án. Nếu tiếp tục bổ sung kiểm thử, ảnh giao diện, sơ đồ UML và tài liệu vận hành, đề tài có thể trở thành một bản khóa luận hoàn chỉnh, có tính ứng dụng và có khả năng demo thuyết phục trước giảng viên."),
    p("TÀI LIỆU THAM KHẢO NỘI BỘ", "Heading1", page_break_before=True),
]

for item in [
    "Mã nguồn backend: cinema-booking-system/src/main/java/com/cinema/booking.",
    "Mã nguồn frontend: frontend/cinema-client/src.",
    "README backend: hướng dẫn chạy Spring Boot, Swagger, Mailtrap và luồng test booking/payment.",
    "pom.xml: cấu hình Spring Boot, Java 21, JPA, Security, WebSocket, Mail, Thymeleaf, OpenAPI.",
    "package.json frontend: cấu hình React, Vite, TypeScript và các thư viện giao diện.",
    "application.yaml: cấu hình datasource, JPA, mail, VNPay, JWT và logging.",
    "mock-data.sql: dữ liệu mẫu phim, rạp, phòng, ghế, suất chiếu, promotion và user test.",
]:
    chapters.append(bullet(item))

chapters += [
    p("PHỤ LỤC A. TÓM TẮT CẤU TRÚC MÃ NGUỒN", "Heading1", page_break_before=True),
    p("Backend có 154 file Java trong src/main/java. Frontend có 56 file trong src. Các con số này được ghi nhận từ workspace tại thời điểm lập báo cáo. Dưới đây là cách đọc nhanh cấu trúc dự án để giảng viên hoặc người bảo trì có thể nắm nhanh vị trí các thành phần."),
    table(backend_modules, [2400, 1200, 5400]),
    p(),
    table(frontend_modules, [2400, 1200, 5400]),
    p("PHỤ LỤC B. LỆNH CHẠY DỰ ÁN", "Heading1", page_break_before=True),
    h2("Backend"),
    code("cd \"D:\\spring boot\\cinema-booking-system\""),
    code("docker-compose up -d"),
    code("mvn spring-boot:run"),
    code("Swagger UI: http://localhost:8080/swagger-ui.html"),
    h2("Frontend"),
    code("cd \"D:\\spring boot\\frontend\\cinema-client\""),
    code("npm install"),
    code("npm run dev"),
    code("Frontend dev server: http://localhost:5173"),
    h2("Kiểm thử backend đã chạy"),
    code("mvn test"),
    code("Kết quả ghi nhận: BUILD SUCCESS, Tests run: 1, Failures: 0, Errors: 0, Skipped: 0."),
    p("PHỤ LỤC C. GỢI Ý NỘI DUNG THUYẾT TRÌNH", "Heading1", page_break_before=True),
]

for item in [
    "Giới thiệu vấn đề đặt vé xem phim và rủi ro đặt trùng ghế.",
    "Trình bày kiến trúc backend/frontend/database/WebSocket.",
    "Demo đăng nhập user, chọn phim, chọn suất chiếu, giữ ghế và tạo booking.",
    "Demo WebSocket bằng cách mở hai trình duyệt cùng một suất chiếu và giữ ghế.",
    "Demo thanh toán mock hoặc VNPay sandbox nếu cấu hình sẵn.",
    "Demo vé QR và màn staff scanner.",
    "Demo admin dashboard và quản lý phim/rạp/suất chiếu/user.",
    "Kết thúc bằng phần đã hoàn thành, hạn chế và hướng phát triển.",
]:
    chapters.append(bullet(item))


document_xml = f'''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"
 xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
<w:body>
{''.join(chapters)}
<w:sectPr>
  <w:pgSz w:w="11906" w:h="16838"/>
  <w:pgMar w:top="1440" w:right="1134" w:bottom="1440" w:left="1134" w:header="720" w:footer="720" w:gutter="0"/>
</w:sectPr>
</w:body>
</w:document>'''

styles_xml = '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:styles xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
  <w:docDefaults>
    <w:rPrDefault><w:rPr><w:rFonts w:ascii="Times New Roman" w:hAnsi="Times New Roman" w:eastAsia="Times New Roman" w:cs="Times New Roman"/><w:sz w:val="26"/></w:rPr></w:rPrDefault>
    <w:pPrDefault><w:pPr><w:spacing w:after="160" w:line="360" w:lineRule="auto"/><w:jc w:val="both"/></w:pPr></w:pPrDefault>
  </w:docDefaults>
  <w:style w:type="paragraph" w:default="1" w:styleId="Normal"><w:name w:val="Normal"/><w:qFormat/></w:style>
  <w:style w:type="paragraph" w:styleId="Title"><w:name w:val="Title"/><w:basedOn w:val="Normal"/><w:qFormat/><w:pPr><w:jc w:val="center"/><w:spacing w:after="240"/></w:pPr><w:rPr><w:b/><w:sz w:val="34"/></w:rPr></w:style>
  <w:style w:type="paragraph" w:styleId="Heading1"><w:name w:val="heading 1"/><w:basedOn w:val="Normal"/><w:next w:val="Normal"/><w:qFormat/><w:pPr><w:spacing w:before="360" w:after="200"/><w:keepNext/><w:outlineLvl w:val="0"/></w:pPr><w:rPr><w:b/><w:sz w:val="30"/><w:color w:val="1F4E79"/></w:rPr></w:style>
  <w:style w:type="paragraph" w:styleId="Heading2"><w:name w:val="heading 2"/><w:basedOn w:val="Normal"/><w:next w:val="Normal"/><w:qFormat/><w:pPr><w:spacing w:before="260" w:after="160"/><w:keepNext/><w:outlineLvl w:val="1"/></w:pPr><w:rPr><w:b/><w:sz w:val="28"/><w:color w:val="2F75B5"/></w:rPr></w:style>
  <w:style w:type="paragraph" w:styleId="Heading3"><w:name w:val="heading 3"/><w:basedOn w:val="Normal"/><w:next w:val="Normal"/><w:qFormat/><w:pPr><w:spacing w:before="200" w:after="120"/><w:keepNext/><w:outlineLvl w:val="2"/></w:pPr><w:rPr><w:b/><w:sz w:val="26"/><w:color w:val="5B9BD5"/></w:rPr></w:style>
  <w:style w:type="paragraph" w:styleId="CodeBlock"><w:name w:val="CodeBlock"/><w:basedOn w:val="Normal"/><w:pPr><w:spacing w:before="80" w:after="80"/><w:ind w:left="360"/></w:pPr><w:rPr><w:rFonts w:ascii="Consolas" w:hAnsi="Consolas"/><w:sz w:val="22"/><w:color w:val="333333"/></w:rPr></w:style>
  <w:style w:type="table" w:styleId="TableGrid"><w:name w:val="Table Grid"/><w:basedOn w:val="TableNormal"/><w:uiPriority w:val="59"/><w:tblPr><w:tblBorders><w:top w:val="single" w:sz="4" w:space="0" w:color="999999"/><w:left w:val="single" w:sz="4" w:space="0" w:color="999999"/><w:bottom w:val="single" w:sz="4" w:space="0" w:color="999999"/><w:right w:val="single" w:sz="4" w:space="0" w:color="999999"/><w:insideH w:val="single" w:sz="4" w:space="0" w:color="999999"/><w:insideV w:val="single" w:sz="4" w:space="0" w:color="999999"/></w:tblBorders></w:tblPr></w:style>
</w:styles>'''

rels_xml = '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties" Target="docProps/core.xml"/>
  <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/extended-properties" Target="docProps/app.xml"/>
</Relationships>'''

doc_rels_xml = '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>'''

content_types_xml = '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
  <Override PartName="/word/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml"/>
  <Override PartName="/docProps/core.xml" ContentType="application/vnd.openxmlformats-package.core-properties+xml"/>
  <Override PartName="/docProps/app.xml" ContentType="application/vnd.openxmlformats-officedocument.extended-properties+xml"/>
</Types>'''

now = datetime.now(UTC).strftime("%Y-%m-%dT%H:%M:%SZ")
core_xml = f'''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<cp:coreProperties xmlns:cp="http://schemas.openxmlformats.org/package/2006/metadata/core-properties"
 xmlns:dc="http://purl.org/dc/elements/1.1/"
 xmlns:dcterms="http://purl.org/dc/terms/"
 xmlns:dcmitype="http://purl.org/dc/dcmitype/"
 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <dc:title>Báo cáo khóa luận hệ thống đặt vé xem phim trực tuyến</dc:title>
  <dc:subject>cinema-booking-system và cinema-client</dc:subject>
  <dc:creator>Codex</dc:creator>
  <cp:lastModifiedBy>Codex</cp:lastModifiedBy>
  <dcterms:created xsi:type="dcterms:W3CDTF">{now}</dcterms:created>
  <dcterms:modified xsi:type="dcterms:W3CDTF">{now}</dcterms:modified>
</cp:coreProperties>'''

app_xml = '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Properties xmlns="http://schemas.openxmlformats.org/officeDocument/2006/extended-properties"
 xmlns:vt="http://schemas.openxmlformats.org/officeDocument/2006/docPropsVTypes">
  <Application>Microsoft Word</Application>
  <DocSecurity>0</DocSecurity>
  <ScaleCrop>false</ScaleCrop>
  <Company></Company>
  <LinksUpToDate>false</LinksUpToDate>
  <SharedDoc>false</SharedDoc>
  <HyperlinksChanged>false</HyperlinksChanged>
  <AppVersion>16.0000</AppVersion>
</Properties>'''


with ZipFile(OUT, "w", ZIP_DEFLATED) as z:
    z.writestr("[Content_Types].xml", content_types_xml)
    z.writestr("_rels/.rels", rels_xml)
    z.writestr("word/_rels/document.xml.rels", doc_rels_xml)
    z.writestr("word/document.xml", document_xml)
    z.writestr("word/styles.xml", styles_xml)
    z.writestr("docProps/core.xml", core_xml)
    z.writestr("docProps/app.xml", app_xml)

print(OUT)
