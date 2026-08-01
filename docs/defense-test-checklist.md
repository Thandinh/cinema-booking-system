# Checklist test bao ve khoa luan - CinemaBooking.vn

Tai lieu nay la checklist test thu cong de demo va kiem tra truoc khi bao ve. Nen chay theo thu tu de tranh du lieu bi lech.

## 1. Chuan bi moi truong

### Backend

- [ ] PostgreSQL dang chay.
- [ ] Database da tao schema bang Flyway hoac `database/database.sql`.
- [ ] Da chay `database/mock-data.sql`.
- [ ] File `.env` backend co cau hinh:
  - [ ] `JWT_SIGNER_KEY`
  - [ ] `BOOKING_SEAT_HOLD_MINUTES`
  - [ ] Mailtrap/SMTP
  - [ ] VNPay sandbox
  - [ ] `APP_BACKEND_URL`
  - [ ] `APP_FRONTEND_URL`
- [ ] Backend chay tren `http://localhost:8080`.

### Frontend

- [ ] File `.env` frontend co:
  - [ ] `VITE_API_BASE_URL`
  - [ ] `VITE_BOOKING_SEAT_HOLD_MINUTES`
  - [ ] `VITE_GOOGLE_CLIENT_ID` neu demo Google login.
- [ ] Frontend chay tren `http://localhost:5173`.

### Ngrok/VNPay neu demo thanh toan

- [ ] Ngrok dang forward backend neu VNPay callback can public URL.
- [ ] `APP_BACKEND_URL` dung URL ngrok backend.
- [ ] VNPay return/callback khong tro ve URL da tat.

## 2. Test dang ky va xac thuc email

### Dang ky thanh cong

- [ ] Mo trang dang ky.
- [ ] Nhap username moi, email moi, password manh, confirm password.
- [ ] Bam dang ky.
- [ ] He thong bao can xac thuc email.
- [ ] Mailtrap nhan email xac thuc.
- [ ] Click link xac thuc.
- [ ] He thong bao xac thuc thanh cong.

Ket qua mong doi:

- `users.email_verified = true`.
- Token hash xac thuc email duoc clear.

### Dang ky trung username/email

- [ ] Dang ky lai username da ton tai.
- [ ] Giao dien hien loi tieng Viet.
- [ ] Dang ky lai email da ton tai.
- [ ] Giao dien hien loi tieng Viet.

Ket qua mong doi:

- Khong tao user moi.
- API tra conflict hop ly.

## 3. Test dang nhap, refresh token, logout

### Dang nhap dung

- [ ] Dang nhap `user1 / 123456`.
- [ ] Vao duoc trang khach hang.
- [ ] Navbar hien thong tin user.
- [ ] Goi API `/api/v1/users/me` thanh cong.

Ket qua mong doi:

- Backend tra access token.
- Refresh token nam trong HttpOnly cookie `cinema_refresh_token`.
- Bang `refresh_tokens` co record moi.
- Bang `auth_audit_logs` co `LOGIN_PASSWORD success=true`.

### Dang nhap sai nhieu lan

- [ ] Nhap sai mat khau lien tuc nhieu lan.
- [ ] Sau nguong gioi han, API tra `429`.
- [ ] UI khong reload trang.
- [ ] Co log auth audit that bai.

Ket qua mong doi:

- `AUTH_RATE_LIMITED`.
- `auth_audit_logs` co failure reason.

### Auto refresh token

Cach test goi y:

- [ ] Dang nhap thanh cong.
- [ ] Cho access token het han hoac tam thoi giam thoi gian access token trong `.env`.
- [ ] Goi API bat ky can auth.
- [ ] Frontend tu goi `/auth/refresh`.
- [ ] Request ban dau duoc retry va van thanh cong.

Ket qua mong doi:

- User khong bi day ve login neu refresh token con hop le.
- Bang `refresh_tokens` record cu co `revoked_reason = ROTATED`.
- Record moi duoc tao.

### Logout

- [ ] Bam dang xuat.
- [ ] Quay ve login.
- [ ] Goi API bang access token cu bi reject.
- [ ] Refresh token cookie bi clear.

Ket qua mong doi:

- `invalidated_token` co jti cua access token.
- `refresh_tokens` record hien tai co `revoked_reason = LOGOUT`.
- `auth_audit_logs` co `LOGOUT`.

## 4. Test quan ly phien dang nhap

### Xem phien dang nhap

- [ ] Dang nhap user tren trinh duyet 1.
- [ ] Dang nhap cung user tren trinh duyet khac/incognito.
- [ ] Vao Ho so ca nhan.
- [ ] Box "Thiet bi dang dang nhap" hien nhieu phien.

Ket qua mong doi:

- Phien hien tai co nhan "Hien tai".
- Co IP, thoi gian, thiet bi/trinh duyet.

### Dang xuat thiet bi khac

- [ ] Bam "Dang xuat thiet bi khac".
- [ ] Reload trinh duyet khac.
- [ ] Trinh duyet khac khong refresh token duoc nua.

Ket qua mong doi:

- Cac refresh token khac co `revoked_reason = OTHER_SESSIONS_REVOKED`.
- Phien hien tai van dung duoc.

### Dang xuat mot phien cu

- [ ] Chon mot session khong phai hien tai.
- [ ] Bam "Dang xuat".
- [ ] Session do bi revoke.

Ket qua mong doi:

- Session do co `revoked_at`.
- UI cap nhat trang thai "Da dang xuat".

## 5. Test xem phim/rap/suat chieu public

- [ ] Chua dang nhap van xem duoc trang Home.
- [ ] Chua dang nhap van xem duoc chi tiet phim.
- [ ] Chua dang nhap van xem duoc danh sach rap.
- [ ] Chua dang nhap van xem duoc lich chieu theo rap/phim.
- [ ] Chua dang nhap bam chon ghe/dat ve thi bi yeu cau login.

Ket qua mong doi:

- Public endpoints khong yeu cau JWT.
- Luong dat ve yeu cau auth.

## 6. Test chon ghe va realtime

### Giu ghe

- [ ] Dang nhap user1.
- [ ] Chon phim/suat chieu.
- [ ] Vao trang chon ghe.
- [ ] Chon 2 ghe.
- [ ] Ghe chuyen sang dang chon/giu.

Ket qua mong doi:

- `seat_status.status = HOLD`.
- `hold_by = user1`.
- `hold_until` co thoi gian het han.

### Realtime 2 trinh duyet

- [ ] Mo cung suat chieu tren user1 va user2.
- [ ] User1 chon ghe.
- [ ] User2 thay ghe do doi trang thai khong can refresh.

Ket qua mong doi:

- WebSocket event cap nhat mau ghe.

### Het han giu ghe

- [ ] Giu ghe nhung khong thanh toan.
- [ ] Doi het thoi gian giu ghe.
- [ ] Ghe tu dong ve AVAILABLE.
- [ ] UI cap nhat, khong can refresh.

Ket qua mong doi:

- `seat_status.status = AVAILABLE`.
- Booking PENDING het han thanh `EXPIRED` neu da tao booking.

## 7. Test dat ve va thanh toan

### Tao booking PENDING

- [ ] Chon ghe.
- [ ] Bam tiep tuc thanh toan.
- [ ] Tao booking thanh cong.

Ket qua mong doi:

- `bookings.status = PENDING`.
- `payment_expires_at` co gia tri.
- `booking_details` co record theo tung ghe.

### Ap ma giam gia

- [ ] Nhap ma hop le, vi du `WELCOME10`.
- [ ] Tong tien giam dung.
- [ ] Nhap ma khong ton tai.
- [ ] Nhap ma het han/khong du dieu kien neu co data.

Ket qua mong doi:

- UI hien tong tien VND dung format.
- Loi ma giam gia hien ro.

### VNPay thanh cong

- [ ] Bam thanh toan VNPay.
- [ ] Sang sandbox VNPay.
- [ ] Thanh toan thanh cong.
- [ ] Quay ve trang ket qua thanh toan.

Ket qua mong doi:

- `payments.status = SUCCESS`.
- `bookings.status = SUCCESS`.
- `seat_status.status = BOOKED`.
- `tickets` duoc tao.
- Email ve duoc gui.
- Ghe realtime chuyen BOOKED.

### VNPay that bai/huy

- [ ] Khoi tao thanh toan.
- [ ] Huy/thanh toan that bai tren sandbox.

Ket qua mong doi:

- `payments.status = FAILED` hoac booking het han thanh `EXPIRED`.
- Ghe duoc tra ve AVAILABLE.
- User khac co the dat lai ghe.

## 8. Test email

### Email xac thuc

- [ ] Dang ky user moi.
- [ ] Mailtrap co email xac thuc.
- [ ] Link xac thuc hoat dong.

### Email ve

- [ ] Thanh toan thanh cong.
- [ ] Mailtrap co email ve.
- [ ] Email co:
  - [ ] Logo cinemabooking.vn.
  - [ ] Ten phim.
  - [ ] Rap/phong.
  - [ ] Dia chi + city.
  - [ ] Thoi gian `HH:mm · dd/MM/yyyy`.
  - [ ] Ghe.
  - [ ] Tong tien VND.
  - [ ] QR tung ve/ghe.

## 9. Test ve cua toi va lich su giao dich

- [ ] User vao "Ve cua toi".
- [ ] Thay ve thanh cong.
- [ ] Thay don PENDING neu co.
- [ ] Thay don EXPIRED/FAILED neu mock data co.
- [ ] Don da ket thuc suat chieu khong cho chon lai ghe.
- [ ] Don con hop le co the tiep tuc thanh toan/chon lai ghe theo rule.

Ket qua mong doi:

- UI khong rung khi dem nguoc.
- Card ve gon, thong tin ro.
- Trang thai ve dung voi DB.

## 10. Test QR check-in staff

### Chuan bi

- [ ] Dang nhap staff.
- [ ] Vao trang Soat ve QR.
- [ ] Chon thanh pho.
- [ ] Chon rap.
- [ ] Chon suat chieu dang mo check-in.

### Pham vi rap cua staff

- [ ] Admin vao Quan ly nguoi dung.
- [ ] Gan staff1 cho mot hoac nhieu rap.
- [ ] Dang nhap staff1 va kiem tra danh sach soat ve/booking/payment chi hien du lieu rap duoc gan.
- [ ] Thu goi API/quet ve cua rap khong duoc gan.

Ket qua mong doi:

- Staff khong thay du lieu rap ngoai pham vi.
- Backend tra `UNAUTHORIZED` neu staff thao tac tren rap khong duoc gan.
- Neu QR hop le nhung chon sai rap/sai suat, backend van bao sai rap/sai suat va khong set USED.

### QR dung

- [ ] Quet QR bang camera.
- [ ] Hoac upload anh QR.

Ket qua mong doi:

- Backend bao xac thuc thanh cong.
- `tickets.status = USED`.
- `check_in_time` co gia tri.
- `checked_in_by = staff`.

### QR da dung

- [ ] Quet lai cung QR.

Ket qua mong doi:

- Bao ve da duoc su dung.
- Hien gio check-in va nhan vien check-in neu co.

### Sai rap/sai suat

- [ ] Chon rap khac roi quet QR dung.
- [ ] Chon suat chieu khac roi quet QR dung.

Ket qua mong doi:

- Sai rap: "Ve khong thuoc rap nay".
- Sai suat: "Ve khong thuoc suat chieu dang soat".
- Ticket khong bi doi sang USED.

### Chua den gio/qua gio check-in

- [ ] Quet ve ngoai cua so check-in.

Ket qua mong doi:

- Bao chua den gio hoac qua thoi gian check-in.
- Ticket van ACTIVE.

## 11. Test admin dashboard va quan ly

### Dashboard

- [ ] Dang nhap admin.
- [ ] Vao Tong quan.
- [ ] Thay tong doanh thu, so ve, so suat chieu, so user.
- [ ] Bieu do doanh thu hien dung.
- [ ] Top phim doanh thu hien dung.

### Quan ly phim

- [ ] Them phim.
- [ ] Sua phim.
- [ ] Xoa mem phim.
- [ ] Tim kiem/phan trang hoat dong.

### Quan ly rap

- [ ] Them rap co city, dia chi, toa do.
- [ ] Map hien rap theo toa do.
- [ ] Sua/xoa rap.

### Quan ly phong/ghe

- [ ] Tao phong.
- [ ] Sinh ghe theo layout.
- [ ] Doi loai ghe Normal/VIP/Couple.
- [ ] Khong cho xoa ghe dang co booking/hold neu co rule.

### Quan ly suat chieu

- [ ] Chon thanh pho truoc.
- [ ] Chon rap theo thanh pho.
- [ ] Chon phong theo rap.
- [ ] Tao suat chieu.
- [ ] Khong cho trung gio trong cung phong.

### Quan ly booking/payment

- [ ] Loc booking theo status.
- [ ] Xem chi tiet booking.
- [ ] Loc payment theo status/method.
- [ ] Xem payment events.
- [ ] Reconciliation neu co.

### Audit log

- [ ] Vao Nhat ky.
- [ ] Thay log thao tac admin.
- [ ] Goi API `/api/v1/admin/auth-audit-logs` de xem auth audit neu can demo bang Swagger/Postman.

## 12. Test bao mat RBAC

### User thuong

- [ ] User goi API admin.
- [ ] Bi `403 You do not have permission`.

### Staff

- [ ] Staff vao duoc Soat ve QR.
- [ ] Staff khong vao duoc quan ly role/permission.
- [ ] Staff khong tao admin.

### Admin

- [ ] Admin vao duoc dashboard va cac trang quan ly.

Ket qua mong doi:

- Permission dung voi role.
- Khong co endpoint nhay cam public nham.

## 13. Test loi thuong gap truoc khi bao ve

- [ ] Tat ngrok roi thanh toan VNPay: biet giai thich vi sao callback/website loi.
- [ ] Sai Google Client ID: biet giai thich `invalid_client`.
- [ ] Mailtrap khong nhan mail: kiem tra SMTP host/port/user/pass.
- [ ] User1 khong dang nhap duoc: kiem tra da chay `ApplicationInitConfig` va `mock-data.sql`, mat khau `123456`, email verified.
- [ ] Ghe het han nhung UI chua doi: kiem tra WebSocket va scheduler.
- [ ] QR file khong doc duoc: kiem tra anh ro, QR khong bi crop/blur.

## 14. Checklist len kich ban demo 10 phut

Thu tu demo de an toan:

1. [ ] Mo Home, xem phim/rap public.
2. [ ] Dang nhap user.
3. [ ] Chon phim, chon suat, giu ghe.
4. [ ] Mo trinh duyet khac xem realtime ghe HOLD.
5. [ ] Ap ma giam gia.
6. [ ] Thanh toan VNPay thanh cong.
7. [ ] Xem Ve cua toi va email ve.
8. [ ] Dang nhap staff, quet QR dung.
9. [ ] Quet lai QR de thay da dung.
10. [ ] Dang nhap admin, xem dashboard, booking/payment, audit.
11. [ ] Vao Ho so user, xem Thiet bi dang dang nhap va dang xuat thiet bi khac.
12. [ ] Dang nhap sai nhieu lan de noi ve rate limit/auth audit neu con thoi gian.

## 15. Cau tra loi ngan khi thay co hoi

### Vi sao can HOLD ghe?

De tranh 2 nguoi cung thanh toan mot ghe. HOLD tao cua so tam thoi, neu thanh toan thanh cong thi BOOKED, neu het han hoac that bai thi AVAILABLE.

### Vi sao moi ghe mot QR?

Vi product that can check-in tung nguoi. Nhom dat 4 ghe co the den le te, moi ve/QR dung mot lan.

### Vi sao can refresh token rotation?

Neu refresh token bi lo, token cu se bi revoke sau moi lan refresh. Neu token cu bi dung lai, he thong phat hien reuse va revoke cac phien active cua user.

### Vi sao can staff chon rap/suat truoc khi quet?

De tranh ve dung nhung vao nham rap/nham suat. Backend chi set USED khi QR dung, ve active, booking success, dung rap, dung suat va trong cua so check-in.

### Vi sao can audit log?

De truy vet ai da thao tac gi, dang nhap that bai/thanh cong, va cac hanh vi bat thuong. Day la yeu cau quan trong cua he thong thuc te.
