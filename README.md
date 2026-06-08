# 🎬 Cinema Booking System — Backend API

> Enterprise-grade REST API cho hệ thống đặt vé xem phim, xây dựng trên Spring Boot 3.5 + PostgreSQL.

---

## 🏗️ Kiến trúc & Công nghệ

| Layer | Technology |
|-------|-----------|
| Framework | Spring Boot 3.5 (Java 21) |
| Database | PostgreSQL + Spring Data JPA |
| Security | Spring Security + JWT (OAuth2 Resource Server) |
| Authorization | RBAC (Role → Permission) |
| Real-time | WebSocket (STOMP over SockJS) |
| Email | Spring Mail + Thymeleaf Template |
| Documentation | SpringDoc OpenAPI (Swagger UI) |
| Build | Maven |

---

## ⚡ Luồng nghiệp vụ chính

```
Chọn ghế → HOLD (Pessimistic Lock, 10 phút)
    ↓
Tạo Booking (PENDING + secureToken)
    ↓
Thanh toán (Mock / VNPay / MoMo)
    ↓
Callback thành công
    ├─ Ghế: HOLD → BOOKED
    ├─ Booking: PENDING → SUCCESS
    ├─ Tạo Ticket + QR Code (đồng bộ)
    ├─ Push WebSocket → tất cả client tự đổi màu ghế (đồng bộ, ms)
    └─ Gửi Email vé + QR (bất đồng bộ @Async)
```

---

## 🚀 Khởi chạy dự án

### Yêu cầu
- Java 21+
- Docker & Docker Compose (để chạy PostgreSQL)
- Maven 3.9+

### Bước 1: Clone & cấu hình môi trường

```bash
# Tạo file .env từ template
cp .env.example .env
```

Mở file `.env` và điền các giá trị:

```env
# Database
DB_NAME=cinema_booking
DB_USER=cinema_user
DB_PASSWORD=123456
DB_HOST=localhost
DB_PORT_EXTERNAL=5433

# Security
JWT_SECRET=your-secret-key-at-least-32-chars

# Email (Mailtrap - Xem hướng dẫn test bên dưới)
MAIL_HOST=sandbox.smtp.mailtrap.io
MAIL_PORT=2525
MAIL_USERNAME=your_mailtrap_username
MAIL_PASSWORD=your_mailtrap_password
```

### Bước 2: Khởi động Database

```bash
docker-compose up -d
```

### Bước 3: Chạy ứng dụng

```bash
mvn spring-boot:run
```

Ứng dụng chạy tại: **http://localhost:8080**

---

## 📖 Tài liệu API (Swagger)

Sau khi ứng dụng chạy, truy cập:

👉 **http://localhost:8080/swagger-ui.html**

**Cách test API có xác thực:**
1. Gọi `POST /auth/token` với username/password
2. Copy token từ response
3. Bấm nút **Authorize 🔒** (góc phải trên)
4. Dán: `Bearer <token_vừa_copy>`
5. Bấm **Authorize** → Close
6. Tất cả API sẽ tự gắn token vào header

---

## 📧 Hướng dẫn Test Email + QR Code

### Bước 1: Đăng ký Mailtrap (miễn phí)

1. Truy cập **https://mailtrap.io** và đăng ký tài khoản miễn phí
2. Vào **Email Testing → Inboxes → My Inbox**
3. Bấm vào inbox → chọn tab **SMTP Settings**
4. Chọn Integration: **Spring Boot**
5. Copy `Host`, `Port`, `Username`, `Password` vào file `.env` của bạn

> **Tại sao Mailtrap?** Mailtrap là "bẫy email" — mọi email bạn gửi đi sẽ bị chặn lại và hiển thị trong dashboard của Mailtrap thay vì bay vào hộp thư thật. Cực kỳ an toàn khi test.

### Bước 2: Chạy luồng test đầy đủ qua Swagger

**Mở Swagger UI: http://localhost:8080/swagger-ui.html**

#### 1️⃣ Đăng nhập lấy token (USER role)
```
POST /auth/token
Body: { "username": "user01", "password": "123456" }
→ Copy token, bấm Authorize trong Swagger
```

#### 2️⃣ Xem danh sách suất chiếu
```
GET /api/v1/showtimes
→ Copy 1 showtimeId từ kết quả
```

#### 3️⃣ Giữ ghế
```
POST /api/v1/bookings/hold
Body: {
  "showtimeId": "<id_suất_chiếu>",
  "seatIds": ["<id_ghế_1>", "<id_ghế_2>"]
}
→ Copy secureToken từ response
```

#### 4️⃣ Tạo booking
```
POST /api/v1/bookings
Body: {
  "showtimeId": "<id_suất_chiếu>",
  "seatIds": ["<id_ghế_1>", "<id_ghế_2>"]
}
→ Copy secureToken từ response
```

#### 5️⃣ Khởi tạo thanh toán (mock)
```
POST /api/v1/payments/initiate
Body: { "bookingId": "<id_booking>", "method": "VNPAY" }
→ Nhận URL giả lập (chứa secureToken)
```

#### 6️⃣ Giả lập Thanh toán Thành công (đây là bước trigger Email!)
```
POST /api/v1/payments/callback/success
Body: { "secureToken": "<secure_token_từ_bước_4>" }
→ Hệ thống sẽ:
   ✅ Đổi trạng thái Booking → SUCCESS
   ✅ Đổi trạng thái Ghế → BOOKED
   ✅ Sinh QR Code cho từng vé
   ✅ Push WebSocket event
   ✅ Gửi Email bất đồng bộ (check Mailtrap sau 2-3 giây)
```

#### 7️⃣ Kiểm tra Email trên Mailtrap
- Vào **https://mailtrap.io** → Inbox
- Bạn sẽ thấy email "🎟️ Xác nhận vé xem phim"
- Mở email → Xem ảnh QR Code được render tự động

#### 8️⃣ Xem vé trong tài khoản
```
GET /api/v1/tickets/my
→ Xem toàn bộ vé đã mua, bao gồm mã QR
```

#### 9️⃣ Check-in (Staff role)
```
POST /api/v1/tickets/check-in?qrCode=<mã_qr>
→ Vé chuyển sang USED, lưu check_in_time
```

---

## 🔐 Hệ thống phân quyền (RBAC)

| Role | Quyền hạn chính |
|------|----------------|
| **ADMIN** | Toàn quyền hệ thống |
| **STAFF** | Xem booking, quét vé check-in, xem dashboard |
| **USER** | Xem phim, đặt vé, thanh toán, xem vé của mình |

---

## ⚡ Real-time WebSocket

Frontend kết nối và nhận cập nhật ghế theo thời gian thực:

```javascript
// Thư viện: @stomp/stompjs
const client = new Client({ brokerURL: 'ws://localhost:8080/ws' });

client.onConnect = () => {
  // Subscribe vào kênh của suất chiếu đang xem
  client.subscribe(`/topic/seatmap/${showtimeId}`, (msg) => {
    const event = JSON.parse(msg.body);
    // event = { seatId, status: "HOLD"/"BOOKED"/"AVAILABLE", holdUntil, ... }
    updateSeatColor(event.seatId, event.status);
  });
};

client.activate();
```

**Event được push tự động khi:**
- User giữ ghế → `HOLD`
- Thanh toán thành công → `BOOKED`
- Thanh toán thất bại / hủy đơn → `AVAILABLE`
- Scheduler tự động nhả ghế hết hạn (mỗi 60s) → `AVAILABLE`

---

## 📁 Cấu trúc thư mục

```
src/main/java/com/cinema/booking/
├── configuration/      # Cấu hình Spring (OpenAPI, JWT, ...)
├── controller/         # REST Controllers
├── dto/                # Request & Response DTOs
├── entity/             # JPA Entities
├── enums/              # Enums (Status, Type, ...)
├── exception/          # Exception handling (GlobalExceptionHandler)
├── mapper/             # Entity ↔ DTO Mappers
├── repository/         # Spring Data JPA Repositories
├── security/           # JWT, SecurityConfig, Scheduler
├── service/            # Service Interfaces & Implementations
├── util/               # SecurityUtils, ...
└── websocket/          # WebSocket Config, Publisher, Event DTO
```

---

## 🛣️ Lộ trình phát triển tiếp theo

- [ ] **VNPay/MoMo Integration** — Thay thế mock payment bằng cổng thanh toán thật
- [ ] **Dashboard & Reports** — API thống kê doanh thu, top phim, occupancy rate
- [ ] **Push Notification** — Thông báo đẩy qua Firebase (cho mobile app)
- [ ] **Unit & Integration Tests** — Kiểm thử tự động cho `holdSeats`, `handlePaymentSuccess`
