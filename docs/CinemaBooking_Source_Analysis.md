# CinemaBooking Source Analysis

## 1. Phạm vi và nguyên tắc

Tài liệu được reverse-engineer từ source code hiện tại: backend Spring Boot, frontend React, Flyway migration, application.yaml, docker-compose.yml và test source. Không dùng báo cáo khóa luận cũ làm source of truth. Những điều source không thể chứng minh được đều được đánh dấu `[CHƯA XÁC MINH ĐƯỢC TỪ SOURCE CODE]`.

## 2. Bản đồ source đã khảo sát

- Controllers: 14
- Service/interface/implementation: 33
- Repository/projection: 25
- Entity: 23
- Flyway migrations: 14
- React pages: 30
- Java test classes: 12
- REST endpoints extracted: 101

## 3. Kiến trúc tổng thể

`React Pages/Components → src/api Axios client → Spring Controller → Service → Repository/Projection → PostgreSQL`

Các nhánh ngoài luồng REST thông thường: frontend subscribe STOMP `/topic/seatmap/{showtimeId}`; scheduler chạy trong backend; VNPay callback và SePay webhook quay lại PaymentController; EmailService chạy bất đồng bộ cho các thông báo phù hợp.

## 4. Frontend React

- `src/router/AppRouter.tsx`: lazy route, ba layout Auth/Admin/Public và `ProtectedRoute` theo permission.
- `src/api/axiosClient.ts`: đính Bearer token; gộp các yêu cầu 401 vào một `refreshPromise`; retry đúng một lần sau refresh; logout khi refresh thất bại.
- `src/stores/authStore.ts`: chỉ lưu access token, user, permission trong localStorage; refresh token bị xóa khỏi localStorage. Đây là bằng chứng frontend ưu tiên session refresh phía server/cookie (`withCredentials: true`).
- `src/hooks/useSeatWebSocket.ts`: native WebSocket STOMP `/ws-native`, subscribe theo showtime, reconnect 3 giây, cleanup unmount và giữ callback bằng ref để tránh resubscribe thừa.
- `src/pages/public/*`, `src/pages/user/*`, `src/pages/admin/*`, `src/pages/staff/*`: tách màn hình public, customer, admin và staff.
- `src/components/RegionalShowtimeBrowser.tsx`: dùng React Query cho truy vấn lịch chiếu vùng; `App.tsx` có ErrorBoundary toàn app.

## 5. Backend Spring Boot

- `configuration/SecurityConfig.java`: permit có chủ đích cho browse public, auth public, callback/webhook và WebSocket handshake; còn lại yêu cầu authentication. `@EnableMethodSecurity` bật `@PreAuthorize`.
- `security/service/AuthenticationService.java`: password BCrypt, Google ID token, claim `token_use`, `auth_version`, token invalidation, refresh token hash/rotation, rate limit và audit.
- `service/impl/BookingServiceImpl.java`: owner check, hold/booking/cancel/promotion, ticket and check-in; là trung tâm lifecycle booking/seat.
- `service/impl/PaymentServiceImpl.java`: điều phối payment methods, gateway validation, idempotency, event recording and post-payment side effects.
- `payment/*PaymentGateway.java`: adapter theo provider. VnPay/SePay được cấu hình; Momo class tồn tại nhưng default disabled.
- `exception/GlobalExceptionHandler.java`: chuẩn hóa lỗi cho client. DTO dùng Bean Validation.
- `configuration/CacheConfig.java` và service annotations: Caffeine cho dữ liệu đọc ít đổi; không cache seat status.
- `security/task/*Scheduler.java`: cleanup and time-derived state synchronization.

## 6. Database và migration

Flyway được bật với `classpath:db/migration`, `baseline-on-migrate=true`, `validate-on-migrate=true`; JPA `ddl-auto=validate`, do đó schema được kiểm soát bằng migration thay vì JPA tự tạo production schema.

### 6.1. Domain entities

AdminAuditLog, AuthAuditLog, BaseEntity, Booking, BookingDetail, Cinema, InvalidatedToken, Movie, Payment, PaymentEvent, Permission, Promotion, RefreshToken, Refund, Role, Room, Seat, SeatStatus, Showtime, StaffCinema, StaffCinemaId, Ticket, User.

### 6.2. Tính toàn vẹn và index

- `seat_status` có unique `(seat_id, showtime_id)`, CHECK cho HOLD và `version` để bảo vệ cạnh tranh.
- `bookings`, `payments`, `tickets`, `refunds` có trạng thái CHECK/enum, foreign key và index theo truy vấn vận hành.
- V13/V14 bổ sung partial unique pending payment/booking để chống nhân đôi khi double click, callback chậm hoặc retry.
- Index theo thời gian cho scheduler, payment events, audit, showtimes và filters admin nằm trong V1, V2, V3, V4, V5, V9, V10, V11, V13, V14.

## 7. Traceability các luồng trọng yếu

| Luồng | Chuỗi file/class | Điều được xác nhận |
| --- | --- | --- |
| Authentication | `AuthenticationController.java → AuthenticationService.java → UserRepository/RefreshTokenRepository/InvalidatedTokenRepository → users/refresh_tokens/invalidated_token/auth_audit_logs` | Password login, Google ID token verification, JWT issue/verify, refresh rotation, logout and session revoke. |
| Authorization | `SecurityConfig.java → CustomJwtDecoder.java → @PreAuthorize on Controller → StaffCinemaScopeService.java` | Filter-level authentication, permission-level access, then cinema scope for staff operations. |
| Seat hold/booking | `BookingController.java → BookingServiceImpl.java → SeatStatusRepository/BookingRepository → seat_status/bookings/booking_details` | Rate limit, expiry cleanup, lock/availability check, HOLD/PENDING state, transaction and after-commit event. |
| VNPay | `PaymentController.java → PaymentServiceImpl.java → VnPayPaymentGateway.java → PaymentRepository/PaymentEventRepository` | Initiate URL, checksum/amount verification, idempotent success/failure handling. |
| SePay | `PaymentController.java → PaymentServiceImpl.java → SePayPaymentGateway.java → PaymentEventServiceImpl` | Build bank QR, public webhook with API key/HMAC check, amount/content reconciliation. |
| Ticket/check-in | `TicketController.java → BookingServiceImpl.java → TicketRepository/ShowtimeService` | Signed QR, ACTIVE/SUCCESS/context/window/scope check before USED. |
| Realtime | `WebSocketConfig.java + SeatStatusPublisher.java → frontend useSeatWebSocket.ts` | STOMP topic /topic/seatmap/{showtimeId}; frontend only updates UI after event. |
| Schedulers | `HoldExpireScheduler.java; PendingBookingExpireScheduler.java; ShowtimeStatusSyncScheduler.java` | Release expired holds, expire pending bookings, synchronize showtime status. |
| Refund request/audit | `RefundServiceImpl.java; PaymentEventServiceImpl.java; AdminAuditLogInterceptor.java; AuthAuditService.java` | Internal refund-request lifecycle, operator-recorded result, payment-event history and operational/authentication trace; no provider refund API. |

## 8. Concurrency và race condition

`SeatStatus` là nguồn trạng thái ghế theo suất. Luồng hold/confirm được bọc transaction trong service, sử dụng repository lock/query và kiểm tra trạng thái; schema có unique seat-showtime, version và partial unique pending booking/payment. Response 'seat not available' khi cạnh tranh là lỗi nghiệp vụ dự kiến, không phải lỗi hệ thống. Scheduler thu hồi HOLD/PENDING hết hạn; publisher chỉ broadcast sau commit để client không thấy dữ liệu rollback.

## 9. Payment và idempotency

VNPay và SePay có flow riêng. VNPay dựa callback/checksum; SePay dựa webhook/API key-HMAC/amount-content. PaymentEvent lưu event nhằm trace webhook, signature failure, mismatch, reused/processed payment và thay đổi trạng thái yêu cầu refund. RefundServiceImpl chỉ tạo yêu cầu và ghi nhận kết quả do operator cung cấp; source không có lời gọi provider refund API. Không có bằng chứng source về hàng đợi message broker hoặc reconciliation worker bên ngoài process; source hiện dùng repository/service/scheduler/event table.

## 10. Phân loại mức độ hoàn thiện

| Trạng thái | Hạng mục | Căn cứ |
| --- | --- | --- |
| Đã hiện thực | Auth/JWT/refresh/session, RBAC, staff scope, catalog, seat hold, booking, promotion, VNPay, SePay, ticket QR/check-in, email, WebSocket, scheduler, quản lý yêu cầu refund, audit, cache, tests | Controller/service/entity/migration/frontend/test tương ứng; refund chỉ ở mức yêu cầu/trạng thái do operator ghi nhận. |
| Có cấu trúc nhưng cần cấu hình/kiểm thử triển khai | Google login, SMTP, VNPay, SePay, MoMo gateway | Có class/config; hoạt động thực tế phụ thuộc secrets/provider. `MOMO_ENABLED=false` mặc định. |
| Hướng phát triển | Distributed cache/broker, queue, provider refund API tự động, CI/CD, production monitoring/metrics, kiểm thử tải JMeter dài hạn và xác định capacity | [CHƯA XÁC MINH ĐƯỢC TỪ SOURCE CODE]. |

## 11. API consistency findings

- Inventory được trích từ controller, không tự đặt endpoint.
- Frontend API clients dùng cùng base route `/api/v1` và `/auth`; route bảo vệ UI bổ sung cho server authorization, không thay thế server authorization.
- Callback/webhook không có caller frontend là đúng thiết kế: gateway bên ngoài mới gọi các endpoint đó.
- Có hai endpoint đọc sơ đồ ghế cùng gọi `bookingService.getSeatMap`; frontend đang gọi `/api/v1/showtimes/{id}/seats`. Route alias dưới BookingController chưa có bằng chứng deprecation trong source, vì vậy cần chọn route chuẩn và deprecate có kiểm soát trước khi xóa alias.
- Có hai endpoint check-in cùng đi vào `bookingService.checkInTicket`; frontend gọi `/api/v1/tickets/check-in`. Đây là điểm cần hợp nhất API theo ticket domain, giữ alias có thời hạn nếu cần tương thích client cũ.
- `movieApi.getAll` không truyền keyword xuống backend trong file hiện tại; comment cho biết keyword có thể lọc client-side. Đây là quyết định UX/client hiện có, không phải endpoint mismatch, nhưng cần cân nhắc backend search khi catalog lớn.
- [CHƯA XÁC MINH ĐƯỢC TỪ SOURCE CODE] tất cả caller gián tiếp/dynamic của React ngoài `src/api` vì inventory chỉ dò static API client theo yêu cầu.

## 12. Danh sách test source

- `src/test/java/com/cinema/booking/controller/BookingPaymentSecurityIntegrationTest.java`
- `src/test/java/com/cinema/booking/controller/GlobalExceptionHandlerIntegrationTest.java`
- `src/test/java/com/cinema/booking/payment/SePayPaymentGatewayTest.java`
- `src/test/java/com/cinema/booking/security/service/AuthenticationServiceIntegrationTest.java`
- `src/test/java/com/cinema/booking/security/service/FixedWindowRateLimitServiceTest.java`
- `src/test/java/com/cinema/booking/service/BookingWorkflowIntegrationTest.java`
- `src/test/java/com/cinema/booking/service/HomeShowtimeFeedIntegrationTest.java`
- `src/test/java/com/cinema/booking/service/PaymentCallbackIntegrationTest.java`
- `src/test/java/com/cinema/booking/service/QrCodeImageServiceTest.java`
- `src/test/java/com/cinema/booking/service/TicketQrCodeServiceTest.java`
- `src/test/java/com/cinema/booking/service/UserManagementIntegrationTest.java`
- `src/test/java/com/cinema/booking/support/PostgresIntegrationTest.java`
