# Cinema Booking System

Tai lieu nay huong dan chay day du he thong dat ve xem phim gom:

- Backend: Spring Boot API tai `D:\spring boot\cinema-booking-system`
- Frontend: React/Vite client tai `D:\spring boot\frontend\cinema-client`
- Database: PostgreSQL

He thong ho tro dat ve realtime, giu ghe co timeout, thanh toan VNPay, gui email xac thuc/ticket, Google Login, RBAC admin/staff/user va soat ve QR bang camera hoac file anh.

## 1. Yeu Cau Cai Dat

Can cai san:

- Java 21+
- Maven 3.9+ hoac dung `mvnw.cmd`
- Node.js 20+ va npm
- Docker Desktop
- PostgreSQL client tuy chon: pgAdmin, DBeaver, psql
- ngrok tuy chon khi demo tren dien thoai hoac test callback public

Kiem tra nhanh:

```powershell
java -version
node -v
npm -v
docker --version
ngrok version
```

## 2. Cau Truc Thu Muc

```text
D:\spring boot\
+-- cinema-booking-system\          Backend Spring Boot
|   +-- database\
|   |   +-- database.sql            Tao schema day du
|   |   +-- mock-data.sql           Du lieu mau de demo/test
|   |   +-- rbac-permissions.sql    Dong bo role/permission
|   +-- src\
|   |   +-- main\resources\db\migration\  Flyway migrations
|   +-- docker-compose.yml
|   +-- .env.example
|   +-- README.md
+-- frontend\
    +-- cinema-client\              Frontend React/Vite
        +-- src\
        +-- vite.config.ts
        +-- .env.example
        +-- package.json
```

## 3. Chay Backend

Di chuyen vao backend:

```powershell
cd "D:\spring boot\cinema-booking-system"
```

Tao file `.env` tu mau:

```powershell
Copy-Item .env.example .env
```

Mo `.env` va dien cac gia tri can thiet. Toi thieu de chay local:

```env
DB_NAME=cinema_booking
DB_USER=cinema_user
DB_PASSWORD=123456
DB_HOST=localhost
DB_PORT_EXTERNAL=5433
DB_PORT_INTERNAL=5432

SERVER_PORT=8080
APP_FRONTEND_URL=http://localhost:5173
APP_BACKEND_URL=http://localhost:8080

JWT_SECRET=change-me-to-a-long-random-secret-at-least-32-chars
JWT_EXPIRATION=86400000
```

Khoi dong PostgreSQL bang Docker:

```powershell
docker compose up -d
```

Chay backend:

```powershell
.\mvnw.cmd spring-boot:run
```

Backend mac dinh chay tai:

```text
http://localhost:8080
```

Swagger UI:

```text
http://localhost:8080/swagger-ui.html
```

## 4. Tao Database Va Seed Du Lieu

Backend dung Flyway de tao va nang cap schema tu cac file trong:

```text
src/main/resources/db/migration
```

Voi database moi, chi can khoi dong backend. Flyway se tao bang, index, trigger, sau do Hibernate validate entity.

File `database/database.sql` chi dung khi muon reset database sach bang tay:

```powershell
psql -h localhost -p 5433 -U cinema_user -d cinema_booking -f database/database.sql
```

Sau khi backend da chay it nhat mot lan de dam bao RBAC/admin duoc seed, nap du lieu mau:

```powershell
psql -h localhost -p 5433 -U cinema_user -d cinema_booking -f database/mock-data.sql
```

File `mock-data.sql` se tao:

- Phim, rap, phong, ghe, lich chieu
- Promotion
- User test
- Booking/ticket mau de test QR
- Cac case don hang: thanh cong, dang cho thanh toan, that bai, het han, da huy

Khong de `SQL_INIT_MODE=always` trong product. Gia tri khuyen dung la:

```env
SQL_INIT_MODE=never
FLYWAY_ENABLED=true
```

## 5. Tai Khoan Test

| Role | Username | Password | Ghi chu |
|---|---|---|---|
| ADMIN | `admin` | `admin123` hoac gia tri `app.admin.default-password` | Tao boi `ApplicationInitConfig` |
| STAFF | `staff1` | `123456` | Soat ve QR, quan ly nghiep vu |
| USER | `user1` | `123456` | Dat ve, thanh toan, xem ve |
| USER | `user2` | `123456` | Test multi-user |

Neu login `staff1/user1` khong duoc, hay chay backend truoc de seed role, sau do chay lai `database/mock-data.sql`.

## 6. Chay Frontend

Di chuyen vao frontend:

```powershell
cd "D:\spring boot\frontend\cinema-client"
```

Tao file `.env` tu mau:

```powershell
Copy-Item .env.example .env
```

Gia tri local khuyen dung:

```env
BACKEND_PROXY_TARGET=http://localhost:8080
DEV_SERVER_HOST=localhost
DEV_SERVER_PORT=5173
DEV_ALLOWED_HOSTS=
VITE_API_BASE_URL=
VITE_GOOGLE_CLIENT_ID=your-google-client-id.apps.googleusercontent.com
VITE_BOOKING_SEAT_HOLD_MINUTES=2
```

Cai package neu chua co:

```powershell
npm install
```

Chay frontend:

```powershell
npm run dev
```

Mo web:

```text
http://localhost:5173
```

Build production:

```powershell
npm run build
```

Lint:

```powershell
npm run lint
```

## 7. Proxy Frontend La Gi

Frontend goi API bang URL tuong doi, vi du:

```text
/api/v1/movies
```

Khi chay dev, Vite nhan request nay va proxy ve backend:

```text
http://localhost:8080/api/v1/movies
```

Bien quan trong:

- `VITE_API_BASE_URL`: URL API ma browser/React goi truc tiep. De rong khi muon di qua Vite proxy.
- `BACKEND_PROXY_TARGET`: URL backend ma Vite dev server se chuyen tiep den.

Khuyen dung local/demo:

```env
VITE_API_BASE_URL=
BACKEND_PROXY_TARGET=http://localhost:8080
```

Khuyen dung khi deploy frontend/backend rieng domain:

```env
VITE_API_BASE_URL=https://api.cinemabooking.vn
```

Khi do production web server/API gateway se xu ly routing, Vite proxy khong con duoc dung.

## 8. Cau Hinh Email

He thong dung email cho:

- Xac thuc tai khoan khi dang ky
- Quen mat khau
- Gui ve dien tu kem thong tin ve va QR

Moi truong dev nen dung Mailtrap:

```env
MAIL_HOST=sandbox.smtp.mailtrap.io
MAIL_PORT=2525
MAIL_USERNAME=your-mailtrap-username
MAIL_PASSWORD=your-mailtrap-password
```

Sau khi dang ky user moi hoac thanh toan thanh cong, vao Mailtrap inbox de xem email.

Production co the doi sang SMTP that, vi du Gmail/SendGrid/Mailgun. Khong commit secret email vao git.

## 9. Cau Hinh Google Login

He thong hien dung luong:

```text
React Google Identity Services -> nhan idToken -> POST /auth/google -> Spring Boot verify token -> cap JWT he thong
```

Can cau hinh cung mot Google Client ID o ca hai phia:

Backend `.env`:

```env
GOOGLE_CLIENT_ID=your-google-client-id.apps.googleusercontent.com
```

Frontend `.env`:

```env
VITE_GOOGLE_CLIENT_ID=your-google-client-id.apps.googleusercontent.com
```

Trong Google Cloud Console, them origin cho local:

```text
http://localhost:5173
```

Neu demo qua ngrok, them origin HTTPS cua ngrok:

```text
https://your-ngrok-domain.ngrok-free.dev
```

He thong khong bat buoc dung route `/oauth2/authorization/google` hoac `/login/oauth2/code/google` vi backend khong xu ly redirect OAuth2 theo kieu Spring OAuth2 Client.

## 10. Cau Hinh Thanh Toan

### VNPay Sandbox

Backend `.env`:

```env
VNP_TMN_CODE=your-vnpay-tmn-code
VNP_HASH_SECRET=your-vnpay-hash-secret
VNP_URL=https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
VNP_RETURN_URL=http://localhost:8080/api/v1/payments/vnpay-callback
```

Khi test callback tren dien thoai/ngrok, `VNP_RETURN_URL` nen la URL public HTTPS cua backend hoac route proxy phu hop.

### MoMo

MoMo dang co cau hinh san nhung mac dinh tat:

```env
MOMO_ENABLED=false
```

Chi bat khi co key sandbox/business hop le:

```env
MOMO_ENABLED=true
MOMO_PARTNER_CODE=...
MOMO_ACCESS_KEY=...
MOMO_SECRET_KEY=...
```

## 11. Test Tren Dien Thoai Bang 1 Ngrok Tunnel

Vi ngrok free thuong chi co 1 tunnel, cach gon nhat la public frontend va de Vite proxy ve backend local.

Frontend `.env`:

```env
BACKEND_PROXY_TARGET=http://localhost:8080
DEV_SERVER_HOST=0.0.0.0
DEV_SERVER_PORT=5173
DEV_ALLOWED_HOSTS=your-ngrok-domain.ngrok-free.dev
VITE_API_BASE_URL=
```

Chay frontend:

```powershell
npm run dev
```

Chay ngrok:

```powershell
ngrok http 5173
```

Mo tren dien thoai:

```text
https://your-ngrok-domain.ngrok-free.dev
```

Luu y:

- Camera tren mobile can HTTPS, nen phai dung ngrok hoac domain HTTPS.
- Neu doi `.env` hoac `vite.config.ts`, phai restart Vite.
- Google Login qua ngrok can them ngrok origin vao Google Cloud Console.
- Neu payment gateway can callback ve backend, can dam bao callback URL public truy cap duoc backend.

## 12. Luong Nghiep Vu Chinh

### User dat ve

1. Xem phim/rap/lich chieu.
2. Chon ghe.
3. Ghe chuyen `AVAILABLE -> HOLD`.
4. Tao booking `PENDING`.
5. Thanh toan VNPay.
6. Thanh cong:
   - booking `SUCCESS`
   - payment `SUCCESS`
   - seat `BOOKED`
   - sinh ticket QR
   - gui email ve
   - day realtime seat map
7. That bai/het han/huy:
   - booking `FAILED`, `EXPIRED` hoac `CANCELLED`
   - ghe tra ve `AVAILABLE`

### Staff soat ve QR

1. Staff dang nhap.
2. Chon thanh pho, rap, suat chieu dang mo check-in.
3. Quet QR bang camera hoac tai anh QR.
4. Backend kiem tra:
   - QR hop le
   - ticket con `ACTIVE`
   - booking `SUCCESS`
   - dung rap
   - dung suat chieu
   - dung cua so check-in
5. Hop le thi ticket chuyen `USED`, luu gio va nhan vien soat ve.

### Admin quan ly

Admin co cac khu vuc:

- Tong quan/dashboard
- Phim
- Rap
- Phong/ghe
- Suat chieu
- Don dat ve
- Thanh toan
- Khuyen mai
- Nguoi dung
- Audit log

## 13. Bien Cau Hinh Quan Trong

Backend:

| Bien | Y nghia |
|---|---|
| `BOOKING_SEAT_HOLD_MINUTES` | Thoi gian giu ghe tren seat map |
| `BOOKING_PENDING_TIMEOUT_MINUTES` | Thoi gian booking cho thanh toan |
| `BOOKING_EXPIRED_HOLD_SCAN_DELAY_MS` | Chu ky quet ghe HOLD het han |
| `BOOKING_EXPIRED_BOOKING_SCAN_DELAY_MS` | Chu ky quet booking PENDING het han |
| `SHOWTIME_PUBLIC_DAYS_AHEAD` | So ngay lich chieu public |
| `SHOWTIME_BOOKING_CUTOFF_MINUTES` | Khong cho dat ve sat gio chieu |
| `TICKET_CHECK_IN_EARLY_MINUTES` | Mo check-in truoc gio chieu |
| `TICKET_CHECK_IN_LATE_MINUTES` | Cho check-in sau gio chieu |
| `APP_FRONTEND_URL` | URL frontend de tao link email |
| `APP_BACKEND_URL` | URL backend public cho callback/link khi can |
| `SQL_INIT_MODE` | Nen de `never`; Flyway la co che migrate schema chinh |
| `FLYWAY_ENABLED` | Bat/tat Flyway migration |
| `FLYWAY_BASELINE_ON_MIGRATE` | Ho tro gan baseline cho DB cu chua co Flyway history |

Frontend:

| Bien | Y nghia |
|---|---|
| `VITE_API_BASE_URL` | API base URL public cho browser; de rong khi dung proxy |
| `BACKEND_PROXY_TARGET` | Backend target cho Vite dev proxy |
| `DEV_SERVER_HOST` | Host cua Vite dev server |
| `DEV_ALLOWED_HOSTS` | Domain duoc phep truy cap Vite dev server |
| `VITE_GOOGLE_CLIENT_ID` | Google Client ID cho Google Identity Services |

## 14. Cac Lenh Thuong Dung

Backend:

```powershell
cd "D:\spring boot\cinema-booking-system"
docker compose up -d
.\mvnw.cmd spring-boot:run
.\mvnw.cmd test
.\mvnw.cmd clean package
```

Frontend:

```powershell
cd "D:\spring boot\frontend\cinema-client"
npm install
npm run dev
npm run build
npm run lint
```

Reset database mau:

```powershell
cd "D:\spring boot\cinema-booking-system"
psql -h localhost -p 5433 -U cinema_user -d cinema_booking -f database/database.sql
psql -h localhost -p 5433 -U cinema_user -d cinema_booking -f database/mock-data.sql
```

## 15. Testing

Backend co 2 nhom test:

- Unit test nhe: khong can database, chay nhanh.
- Integration/API test: can Docker Desktop, tu tao PostgreSQL tam bang Testcontainers.

Chay toan bo backend test:

```powershell
cd "D:\spring boot\cinema-booking-system"
.\mvnw.cmd test
```

Chay gon it log:

```powershell
.\mvnw.cmd -q test
```

Chay unit test nhanh khi khong mo Docker:

```powershell
.\mvnw.cmd -Dtest=TicketQrCodeServiceTest,QrCodeImageServiceTest test
```

Chay rieng flow nghiep vu dat ve:

```powershell
.\mvnw.cmd -Dtest=BookingWorkflowIntegrationTest test
```

Chay rieng API/security:

```powershell
.\mvnw.cmd -Dtest=BookingPaymentSecurityIntegrationTest test
```

Chay rieng payment callback:

```powershell
.\mvnw.cmd -Dtest=PaymentCallbackIntegrationTest test
```

Luu y quan trong:

- Integration test khong dung database that trong `.env`.
- Testcontainers tao PostgreSQL rieng trong Docker, chay Flyway migration, seed data test toi thieu roi tu don dep.
- Neu Docker Desktop chua mo, chi nen chay unit test nhanh.
- File `src/test/resources/logback-test.xml` chi dung de giam log khi test, khong anh huong log runtime cua app.

Frontend:

```powershell
cd "D:\spring boot\frontend\cinema-client"
npm run lint
npm run build
```

## 16. Checklist Demo Bao Ve

1. Backend chay khong loi.
2. Frontend chay khong loi.
3. Login duoc `admin`, `staff1`, `user1`.
4. User dat ve, ap ma giam gia, thanh toan thanh cong.
5. Email ve co thong tin rap/phong/ghe/dia chi/QR.
6. Staff scan QR dung rap/suat chieu bang camera.
7. Staff scan QR bang file anh ve.
8. Scan lai ve da dung hien dung thong bao.
9. User xem `Ve cua toi` va `Don da dat`.
10. Admin xem dashboard, quan ly phim/rap/phong-ghe/suat chieu/don/thanh toan/user.
11. Test giu ghe het han va realtime seat map refresh.

## 17. Luu Y Bao Mat Va Trien Khai

- Khong commit file `.env` co secret that.
- Doi mat khau admin mac dinh truoc khi public.
- Dung HTTPS cho camera, Google Login, payment callback.
- Production nen dung reverse proxy/API gateway thay cho Vite proxy.
- Production nen cau hinh SMTP/payment key that bang secret manager hoac bien moi truong server.
- Database production nen backup dinh ky va khong dung `mock-data.sql`.
