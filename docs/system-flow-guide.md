# CinemaBooking.vn - Tai lieu luong hoat dong he thong

Tai lieu nay giai thich cach he thong CinemaBooking.vn van hanh tu frontend den backend, database va cac tich hop ngoai. Muc tieu la giup ban co the trinh bay khi bao ve khoa luan, doc code de hieu nhanh class nao lam viec gi, va biet luong nao di qua endpoint nao.

## 1. Kien truc tong quan

He thong gom 3 lop chinh:

- **Frontend React/Vite**: hien thi giao dien khach hang, admin, staff scanner; goi API qua `axiosClient`.
- **Backend Spring Boot**: xu ly nghiep vu, bao mat JWT/RBAC, dat ve, thanh toan, QR, mail, realtime WebSocket.
- **PostgreSQL**: luu users, RBAC, phim/rap/phong/ghe, suat chieu, booking, payment, ticket, audit log.

Luong tong quat:

```text
Nguoi dung thao tac tren React
  -> axiosClient gan Bearer accessToken
  -> Spring Security xac thuc JWT
  -> Controller nhan request
  -> Service xu ly nghiep vu
  -> Repository thao tac DB
  -> Response ve frontend
```

Neu du lieu ghe thay doi, backend con phat realtime:

```text
Booking/Payment/Scheduler thay doi seat_status
  -> SeatStatusPublisher
  -> WebSocket topic /topic/seatmap/{showtimeId}
  -> Frontend tu cap nhat mau ghe
```

## 2. Cau truc thu muc quan trong

### Backend

- `controller`: noi nhan HTTP request, validate quyen, tra `ApiResponse`.
- `service`: noi chua logic nghiep vu that su.
- `repository`: Spring Data JPA query DB.
- `entity`: mapping bang DB sang Java object.
- `dto/request`: body request tu client gui len.
- `dto/response`: object tra ve client.
- `configuration`: cau hinh app, mail, payment, JWT, init RBAC.
- `security`: JWT, Spring Security, auth service, token cleanup.
- `scheduler`: tac vu nen nhu het han giu ghe/token.
- `db/migration`: Flyway migration tao/cap nhat DB.

### Frontend

- `src/api`: gom cac ham goi API.
- `src/pages`: cac trang UI: Home, SeatSelection, Checkout, Admin, Staff scanner.
- `src/stores`: state global, vi du auth store.
- `src/router`: dinh nghia route va route guard.
- `src/components`: component dung lai.

## 3. Luong bao mat: dang nhap, token, refresh, logout

### 3.1 Dang nhap bang username/password

Class chinh:

- `AuthenticationController`
- `AuthenticationService`
- `AuthRateLimitService`
- `AuthAuditService`
- `RefreshToken`
- `RefreshTokenRepository`

Luong:

```text
React LoginPage
  -> POST /auth/token
  -> AuthenticationController.authenticate()
  -> AuthenticationService.authenticate()
  -> AuthRateLimitService.check()
  -> UserRepository.findByUsername()
  -> BCryptPasswordEncoder.matches()
  -> issueTokenPair()
  -> generateAccessToken()
  -> generateRefreshToken()
  -> luu refresh token hash vao bang refresh_tokens
  -> AuthAuditService ghi LOGIN_PASSWORD success
  -> Controller set HttpOnly cookie cinema_refresh_token
  -> Tra accessToken ve frontend
```

Diem bao mat:

- Access token ngan han, dung de goi API.
- Refresh token dai hon, dung de xin access token moi.
- Refresh token khong tra lo trong JSON response; backend set vao **HttpOnly Cookie**.
- DB chi luu `token_hash`, khong luu token that.
- Neu dang nhap sai qua nhieu lan, `AuthRateLimitService` tra `AUTH_RATE_LIMITED` voi HTTP `429`.
- Moi lan dang nhap thanh cong/thap bai deu ghi vao `auth_audit_logs`.

### 3.2 Dang nhap bang Google

Class chinh:

- `AuthenticationService.authenticateWithGoogle()`
- `decodeGoogleIdToken()`
- `createGoogleUser()`

Luong:

```text
Frontend nhan Google ID token
  -> POST /auth/google
  -> Backend decode ID token bang Google JWK
  -> Kiem tra issuer, audience, email_verified
  -> Neu email da ton tai: cap nhat avatar neu thieu
  -> Neu chua ton tai: tao user moi role USER
  -> Cap access token + refresh cookie nhu dang nhap thuong
```

Diem can giai thich khi bao ve:

- Backend khong tin frontend noi "toi la Google user".
- Backend tu verify ID token voi Google public keys.
- Chi chap nhan email da verified.

### 3.3 Refresh token

Class chinh:

- `AuthenticationController.authenticate()` tai endpoint `/auth/refresh`
- `AuthenticationService.refreshToken()`
- `RefreshTokenRepository`

Luong:

```text
API bat ky tra 401
  -> axiosClient chan response
  -> Goi POST /auth/refresh
  -> Backend doc refresh token tu HttpOnly cookie
  -> verifyToken(refresh=true)
  -> Tim token_hash trong refresh_tokens
  -> Neu token con active:
       - tao refresh token moi
       - revoke refresh token cu voi reason ROTATED
       - tao access token moi
       - set cookie moi
       - tra accessToken moi
  -> axiosClient retry request ban dau
```

Diem toi uu:

- Frontend co `refreshPromise` de nhieu request 401 cung luc chi goi refresh 1 lan.
- Refresh token co rotation, giam rui ro token bi danh cap.
- Neu refresh token da revoke ma bi dung lai, backend danh dau cac refresh token active cua user la `REUSE_DETECTED`.

### 3.4 Logout

Luong:

```text
User bam Dang xuat
  -> POST /auth/logout
  -> Backend invalidate access token vao invalidated_token
  -> Revoke refresh token hien tai
  -> Clear HttpOnly cookie
  -> Frontend clear auth store va localStorage
```

Bang lien quan:

- `invalidated_token`: blacklist access token chua het han nhung da logout.
- `refresh_tokens`: revoke refresh token.
- `auth_audit_logs`: ghi su kien `LOGOUT`.

### 3.5 Quan ly phien dang nhap

Endpoint:

- `GET /auth/sessions`
- `DELETE /auth/sessions/{sessionId}`
- `DELETE /auth/sessions/others`

Frontend:

- `ProfilePage.tsx` hien box **Thiet bi dang dang nhap**.

Luong:

```text
User vao Ho so ca nhan
  -> GET /auth/sessions
  -> Backend lay cac refresh_tokens cua user
  -> Tra ve IP, userAgent, createdAt, expiresAt, revokedAt
  -> User co the dang xuat tung phien hoac cac thiet bi khac
```

Day la tinh nang sat product that vi cac he thong nhu Gmail/Facebook/Banking deu cho quan ly thiet bi dang nhap.

## 4. RBAC va phan quyen

Mo hinh:

```text
users
  -> users_roles
  -> roles
  -> roles_permissions
  -> permissions

staff_cinemas
  -> gan STAFF voi cac rap duoc phu trach
```

Class chinh:

- `ApplicationInitConfig`: seed roles, permissions, admin/staff/user mac dinh.
- `PermissionName`: enum permission.
- `SecurityConfig`: cau hinh endpoint public/private.
- `@PreAuthorize`: chan API theo permission.
- `CustomJwtDecoder`: decode JWT va dua permission vao Spring Security.
- `StaffCinemaScopeService`: gioi han du lieu STAFF theo rap duoc gan.

Vi du:

```java
@PreAuthorize("hasAuthority('MOVIE_CREATE')")
```

Nghia la user phai co permission `MOVIE_CREATE` moi goi duoc endpoint.

Vai tro:

- `ADMIN`: quan tri toan he thong.
- `STAFF`: soat ve QR, xem booking/payment, ho tro rap trong pham vi `staff_cinemas`.
- `USER`: xem phim, dat ve, thanh toan, xem ve cua minh.

## 5. Luong xem phim, rap, suat chieu

Class chinh:

- `MovieController`, `MovieService`
- `CinemaController`, `CinemaService`
- `ShowtimeController`, `ShowtimeService`

Luong khach xem phim:

```text
HomePage
  -> GET /api/v1/movies
  -> Hien danh sach phim
  -> User bam phim
  -> GET /api/v1/showtimes/movie/{movieId}
  -> Chon rap/ngay/suat chieu
```

Luong mua ve theo rap:

```text
CinemaMapPage / CinemaDetailPage
  -> GET /api/v1/cinemas
  -> GET /api/v1/cinemas/map
  -> GET /api/v1/showtimes/cinema/{cinemaId}
```

Product behavior nen co:

- Chi hien suat chieu tu hien tai tro di.
- Nen hien lich chieu khoang 5-7 ngay toi.
- Khach chua dang nhap van duoc xem phim, rap, suat chieu.
- Chi bat dang nhap khi bat dau giu ghe/dat ve.

## 6. Luong chon ghe va giu ghe

Class chinh:

- `BookingController`
- `BookingServiceImpl`
- `SeatStatusRepository`
- `SeatStatusPublisher`
- `HoldExpireScheduler`

Bang chinh:

- `seats`: ghe vat ly cua phong.
- `seat_status`: trang thai ghe theo tung suat chieu.

Trang thai ghe:

- `AVAILABLE`: ghe con trong.
- `HOLD`: ghe dang duoc giu tam.
- `BOOKED`: ghe da thanh toan thanh cong.

Luong giu ghe:

```text
User chon ghe tren SeatSelectionPage
  -> POST /api/v1/bookings/hold
  -> Rate limit kiem tra tan suat theo user va IP
  -> Backend kiem tra ghe AVAILABLE
  -> Update seat_status = HOLD
  -> Gan hold_by = userId
  -> Gan hold_until = now + BOOKING_SEAT_HOLD_MINUTES
  -> WebSocket publish ghe HOLD
  -> Cac man hinh khac cap nhat mau ghe realtime
```

Tai sao can HOLD:

- Tranh 2 nguoi cung thanh toan 1 ghe.
- Cho user co thoi gian di qua cong thanh toan.
- Neu user bo di, scheduler tra ghe ve AVAILABLE.

Het han giu ghe:

```text
HoldExpireScheduler chay dinh ky
  -> Tim seat_status HOLD co hold_until <= now
  -> Update ve AVAILABLE
  -> Booking PENDING het han -> EXPIRED
  -> Payment PENDING lien quan -> EXPIRED neu co
  -> Publish realtime AVAILABLE
```

Bien moi truong lien quan:

- `BOOKING_SEAT_HOLD_MINUTES`
- `BOOKING_HOLD_RATE_LIMIT_ENABLED`
- `BOOKING_HOLD_RATE_LIMIT_USER_MAX_REQUESTS`
- `BOOKING_HOLD_RATE_LIMIT_IP_MAX_REQUESTS`
- `BOOKING_HOLD_RATE_LIMIT_WINDOW_SECONDS`
- `BOOKING_EXPIRED_HOLD_SCAN_DELAY_MS`
- `BOOKING_EXPIRED_BOOKING_SCAN_DELAY_MS`

## 7. Luong tao booking

Class chinh:

- `BookingServiceImpl.createBooking()`
- `BookingDetail`
- `Booking`

Luong:

```text
User da giu ghe
  -> Bam tiep tuc thanh toan
  -> POST /api/v1/bookings
  -> Backend kiem tra ghe HOLD boi dung user
  -> Tao bookings status PENDING
  -> Tao booking_details theo tung ghe
  -> Gan payment_expires_at
  -> Tra secureToken cho frontend
```

Tai sao co `secure_token`:

- Khong expose truc tiep booking id len URL thanh toan.
- Dung de truy cap checkout/payment an toan hon.

## 8. Luong ap ma giam gia

Class chinh:

- `PromotionController`
- `PromotionService`
- `BookingServiceImpl` khi tinh tong tien

Bang:

- `promotions`

Kiem tra ma:

- Ma con active.
- Nam trong `start_date` va `end_date`.
- Don hang dat `min_order_value`.
- Chua vuot `usage_limit`.
- Tinh `discount_amount` theo `PERCENT` hoac `FIXED`.
- Khong vuot `max_discount_amount` neu co.

Product behavior:

- Ma sai/het han phai bao ro ly do.
- Sau thanh toan thanh cong moi tang `used_count`, tranh user nhap ma roi bo thanh toan lam hao quota.

## 9. Luong thanh toan VNPay

Class chinh:

- `PaymentController`
- `PaymentServiceImpl`
- `VnPayService` hoac provider tuong ung
- `PaymentEvent`

Luong thanh toan:

```text
CheckoutPage
  -> POST /api/v1/payments/initiate
  -> Backend tao payment PENDING
  -> Tao URL VNPay co chu ky
  -> Frontend redirect sang sandbox.vnpayment.vn
  -> User thanh toan
  -> VNPay redirect ve /api/v1/payments/vnpay-callback
  -> Backend verify signature
  -> Neu thanh cong:
       payment.status = SUCCESS
       booking.status = SUCCESS
       seat_status = BOOKED
       Tao tickets
       Gui email ve
       Publish realtime BOOKED
  -> Neu that bai:
       payment.status = FAILED
       booking.status = FAILED
       seat_status = AVAILABLE
       Publish realtime AVAILABLE
```

Luu y ngrok:

- VNPay/MoMo can callback/return URL truy cap duoc tu internet.
- Neu tat ngrok, cong thanh toan co the bao khong tim thay giao dich/website.

## 10. Luong gui email

Class chinh:

- Mail service trong backend.
- Template email ticket/verification.

Luong email xac thuc:

```text
User dang ky
  -> Backend tao email_verification_token_hash
  -> Gui link xac thuc qua mail
  -> User click link
  -> Backend hash token va so voi DB
  -> email_verified = true
```

Luong email ve:

```text
Payment SUCCESS
  -> Sinh tickets
  -> Render QR va thong tin ve
  -> Gui email ve cho user
```

Thong tin email ve nen co:

- Ten phim.
- Rap, phong chieu.
- Dia chi rap + city.
- Thoi gian chieu dang `HH:mm · dd/MM/yyyy`.
- Ghe.
- Tong tien VND dung format.
- QR cua tung ve/ghe.

## 11. Luong sinh ve va QR

Class chinh:

- `TicketQrCodeService`
- `QrCodeImageService`
- `TicketController`
- `Ticket`

Thiet ke hien tai:

- Moi `booking_detail` ung voi 1 ghe.
- Moi ghe co 1 ticket.
- Moi ticket co 1 QR rieng.

Day la cach chuan product vi:

- Co the check-in tung nguoi.
- Mot nhom dat nhieu ghe co the vao khac thoi diem.
- QR da dung khong the dung lai.

QR code nen chua:

- Payload co ticket/booking detail identifier.
- Nonce.
- Signature/HMAC de chong gia mao.

Backend khong chi tin text QR; backend verify chu ky va doi chieu DB.

## 12. Luong soat ve QR

Class chinh:

- `TicketController`
- `TicketServiceImpl`
- Staff scanner frontend

Luong chuan:

```text
Staff mo trang Soat ve QR
  -> Chon thanh pho/rap/suat chieu dang mo check-in
  -> Quet camera hoac upload file anh QR
  -> POST /api/v1/tickets/check-in
  -> Backend verify:
       1. QR hop le
       2. Ticket ACTIVE
       3. Booking SUCCESS
       4. Dung cinemaId
       5. Dung showtimeId
       6. STAFF duoc gan voi rap cua ve trong staff_cinemas
       7. Nam trong cua so check-in
  -> Neu tat ca dung:
       ticket.status = USED
       check_in_time = now
       checked_in_by = staff
```

Thong bao loi nen ro:

- Ve khong thuoc rap nay.
- Ve khong thuoc suat chieu dang soat.
- Ve da duoc su dung, kem gio soat va nhan vien soat.
- Chua den gio check-in.
- Qua thoi gian check-in.
- QR khong hop le.

## 13. Luong realtime WebSocket ghe

Class chinh:

- `WebSocketConfig`
- `SeatStatusPublisher`
- `SeatStatusEvent`
- `BookingServiceImpl`
- `HoldExpireScheduler`

Luong:

```text
Frontend vao trang chon ghe
  -> Connect /ws
  -> Subscribe /topic/seatmap/{showtimeId}

User A giu ghe
  -> Backend update DB
  -> SeatStatusPublisher publish HOLD
  -> User B nhan event va ghe doi mau

User A thanh toan thanh cong
  -> publish BOOKED

User A het thoi gian giu ghe
  -> scheduler publish AVAILABLE
```

Tai sao can realtime:

- Khong can refresh trang moi thay ghe doi trang thai.
- Giam xung dot khi nhieu nguoi dat cung suat.
- Tang trai nghiem giong product that.

## 14. Admin dashboard va quan tri

Module admin chinh:

- Dashboard/analytics.
- Quan ly phim.
- Quan ly rap.
- Quan ly phong va ghe.
- Quan ly suat chieu.
- Quan ly don dat ve.
- Quan ly thanh toan.
- Quan ly nguoi dung.
- Quan ly khuyen mai.
- Nhat ky thao tac admin.
- Nhat ky auth/security.

Class chinh:

- `AnalyticsController`
- `AdminAuditLogController`
- `AuthAuditLogController`
- cac controller CRUD tuong ung.

Audit admin ghi:

- Ai thao tac.
- Method/path.
- Resource.
- IP/userAgent.
- Thanh cong/thap bai.

Auth audit ghi:

- Dang nhap thanh cong/thap bai.
- Refresh token.
- Logout.
- Revoke session.
- Ly do loi neu co.

## 15. Database va migration

File quan trong:

- `src/main/resources/db/migration/V1__create_cinema_schema.sql`: schema goc.
- `V2__...`, `V3__...`, `V4__create_refresh_tokens.sql`, `V5__create_auth_audit_logs.sql`: cac thay doi sau.
- `database/database.sql`: file tao database full tu dau.
- `database/mock-data.sql`: seed du lieu demo/test.
- `database/rbac-permissions.sql`: tai lieu/seed RBAC.

Flyway se chay migration theo thu tu version:

```text
V1 -> V2 -> V3 -> V4 -> V5
```

Neu database da co san va them migration moi, chi can restart backend de Flyway apply neu cau hinh Flyway bat.

## 16. Cac diem bao mat noi bat de trinh bay

- JWT access token ngan han.
- Refresh token dai han nhung luu hash trong DB.
- Refresh token rotation.
- Reuse detection cho refresh token.
- Logout blacklist access token.
- RBAC permission-based authorization.
- Email verification truoc khi dang nhap.
- Password BCrypt.
- Rate limit login/refresh.
- Auth audit log.
- Admin audit log.
- QR ticket co chu ky, khong chap nhan QR gia.
- Staff scanner bat chon dung rap/suat chieu truoc khi check-in.
- Payment callback verify signature.
- Khong cho suat chieu qua thoi gian tiep tuc dat ve.

## 17. Cach trinh bay ngan gon khi bao ve

Neu duoc hoi "He thong cua em hoat dong nhu the nao?", co the tra loi:

> He thong gom React frontend, Spring Boot backend va PostgreSQL. Khach co the xem phim, rap, suat chieu public. Khi dat ve, backend giu ghe tam bang trang thai HOLD va day realtime qua WebSocket de cac client khac cap nhat. Khi user thanh toan VNPay thanh cong, backend xac thuc callback, chuyen booking sang SUCCESS, ghe sang BOOKED, sinh ticket QR va gui email. Staff khi soat ve phai chon dung rap va suat chieu, backend verify QR, booking, rap, suat chieu va cua so check-in truoc khi danh dau USED. Phan bao mat dung JWT access/refresh token, refresh token rotation, RBAC permission, rate limit va audit log.

## 18. Cac class nen mo khi demo code

- `SecurityConfig`: endpoint public/private va resource server JWT.
- `AuthenticationService`: dang nhap, refresh token, logout, session.
- `AuthRateLimitService`: chong brute-force.
- `AuthAuditService`: ghi security log.
- `BookingServiceImpl`: giu ghe, tao booking, xu ly payment success/failure.
- `HoldExpireScheduler`: tra ghe khi het han.
- `PaymentServiceImpl`: khoi tao va callback thanh toan.
- `TicketServiceImpl`: verify QR va check-in.
- `SeatStatusPublisher`: realtime trang thai ghe.
- `ApplicationInitConfig`: seed RBAC.
- `V4__create_refresh_tokens.sql`, `V5__create_auth_audit_logs.sql`: migration bao mat moi.
