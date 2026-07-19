-- ========================================================
-- CINEMA BOOKING SYSTEM - RBAC PERMISSIONS
-- File dong bo role/permission cho he thong.
-- Chay lai nhieu lan duoc, khong tao user va khong cham vao du lieu nghiep vu.
-- ApplicationInitConfig van la noi seed RBAC mac dinh khi app khoi dong.
-- ========================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- =========================================
-- 1. PERMISSIONS
-- =========================================
INSERT INTO permissions (id, name, description, created_at, updated_at) VALUES
(uuid_generate_v4(), 'MOVIE_VIEW', 'Xem danh sach phim', NOW(), NOW()),
(uuid_generate_v4(), 'MOVIE_CREATE', 'Them phim', NOW(), NOW()),
(uuid_generate_v4(), 'MOVIE_UPDATE', 'Cap nhat phim', NOW(), NOW()),
(uuid_generate_v4(), 'MOVIE_DELETE', 'Xoa phim', NOW(), NOW()),

(uuid_generate_v4(), 'CINEMA_VIEW', 'Xem thong tin rap', NOW(), NOW()),
(uuid_generate_v4(), 'CINEMA_CREATE', 'Them rap', NOW(), NOW()),
(uuid_generate_v4(), 'CINEMA_UPDATE', 'Cap nhat rap', NOW(), NOW()),
(uuid_generate_v4(), 'CINEMA_DELETE', 'Xoa rap', NOW(), NOW()),

(uuid_generate_v4(), 'ROOM_VIEW', 'Xem phong chieu', NOW(), NOW()),
(uuid_generate_v4(), 'ROOM_CREATE', 'Tao phong chieu', NOW(), NOW()),
(uuid_generate_v4(), 'ROOM_UPDATE', 'Cap nhat phong chieu', NOW(), NOW()),
(uuid_generate_v4(), 'ROOM_DELETE', 'Xoa phong chieu', NOW(), NOW()),

(uuid_generate_v4(), 'SEAT_VIEW', 'Xem ghe', NOW(), NOW()),
(uuid_generate_v4(), 'SEAT_CREATE', 'Tao ghe', NOW(), NOW()),
(uuid_generate_v4(), 'SEAT_UPDATE', 'Cap nhat ghe', NOW(), NOW()),
(uuid_generate_v4(), 'SEAT_DELETE', 'Xoa ghe', NOW(), NOW()),

(uuid_generate_v4(), 'SHOWTIME_VIEW', 'Xem lich chieu', NOW(), NOW()),
(uuid_generate_v4(), 'SHOWTIME_CREATE', 'Tao suat chieu', NOW(), NOW()),
(uuid_generate_v4(), 'SHOWTIME_UPDATE', 'Cap nhat suat chieu', NOW(), NOW()),
(uuid_generate_v4(), 'SHOWTIME_DELETE', 'Xoa suat chieu', NOW(), NOW()),

(uuid_generate_v4(), 'BOOKING_CREATE', 'Dat ve', NOW(), NOW()),
(uuid_generate_v4(), 'BOOKING_VIEW_OWN', 'Xem booking ca nhan', NOW(), NOW()),
(uuid_generate_v4(), 'BOOKING_VIEW_ALL', 'Xem toan bo booking', NOW(), NOW()),
(uuid_generate_v4(), 'BOOKING_CANCEL_OWN', 'Huy booking ca nhan', NOW(), NOW()),
(uuid_generate_v4(), 'BOOKING_CANCEL_ALL', 'Huy booking bat ky', NOW(), NOW()),
(uuid_generate_v4(), 'BOOKING_UPDATE_STATUS', 'Cap nhat trang thai booking', NOW(), NOW()),

(uuid_generate_v4(), 'PAYMENT_CREATE', 'Tao thanh toan', NOW(), NOW()),
(uuid_generate_v4(), 'PAYMENT_VIEW_OWN', 'Xem giao dich ca nhan', NOW(), NOW()),
(uuid_generate_v4(), 'PAYMENT_VIEW_ALL', 'Xem toan bo giao dich', NOW(), NOW()),
(uuid_generate_v4(), 'PAYMENT_REFUND', 'Hoan tien', NOW(), NOW()),

(uuid_generate_v4(), 'PROMOTION_VIEW', 'Xem khuyen mai', NOW(), NOW()),
(uuid_generate_v4(), 'PROMOTION_CREATE', 'Tao khuyen mai', NOW(), NOW()),
(uuid_generate_v4(), 'PROMOTION_UPDATE', 'Cap nhat khuyen mai', NOW(), NOW()),
(uuid_generate_v4(), 'PROMOTION_DELETE', 'Xoa khuyen mai', NOW(), NOW()),

(uuid_generate_v4(), 'USER_VIEW', 'Xem danh sach nguoi dung', NOW(), NOW()),
(uuid_generate_v4(), 'USER_CREATE', 'Tao tai khoan', NOW(), NOW()),
(uuid_generate_v4(), 'USER_UPDATE', 'Cap nhat thong tin nguoi dung', NOW(), NOW()),
(uuid_generate_v4(), 'USER_DELETE', 'Xoa nguoi dung', NOW(), NOW()),
(uuid_generate_v4(), 'USER_BLOCK', 'Khoa tai khoan', NOW(), NOW()),
(uuid_generate_v4(), 'PROFILE_UPDATE', 'Cap nhat ho so ca nhan', NOW(), NOW()),

(uuid_generate_v4(), 'ROLE_MANAGE', 'Quan ly vai tro', NOW(), NOW()),
(uuid_generate_v4(), 'PERMISSION_MANAGE', 'Quan ly quyen', NOW(), NOW()),

(uuid_generate_v4(), 'DASHBOARD_VIEW', 'Xem dashboard', NOW(), NOW()),
(uuid_generate_v4(), 'REPORT_VIEW', 'Xem bao cao thong ke', NOW(), NOW()),
(uuid_generate_v4(), 'ANALYTICS_VIEW', 'Xem du lieu bieu do va phan tich', NOW(), NOW()),

(uuid_generate_v4(), 'TICKET_VIEW_OWN', 'Xem ve ca nhan', NOW(), NOW()),
(uuid_generate_v4(), 'TICKET_VIEW_ALL', 'Xem toan bo ve', NOW(), NOW()),
(uuid_generate_v4(), 'TICKET_CHECKIN', 'Quet QR check-in', NOW(), NOW()),

(uuid_generate_v4(), 'AUTH_LOGIN', 'Dang nhap', NOW(), NOW()),
(uuid_generate_v4(), 'AUTH_LOGOUT', 'Dang xuat', NOW(), NOW()),
(uuid_generate_v4(), 'AUTH_REFRESH_TOKEN', 'Lam moi JWT token', NOW(), NOW())
ON CONFLICT (name) DO UPDATE SET
    description = EXCLUDED.description,
    updated_at = NOW();

-- =========================================
-- 2. ROLES
-- =========================================
INSERT INTO roles (id, name, description, created_at, updated_at) VALUES
(uuid_generate_v4(), 'ADMIN', 'Quan tri toan he thong', NOW(), NOW()),
(uuid_generate_v4(), 'STAFF', 'Nhan vien van hanh rap', NOW(), NOW()),
(uuid_generate_v4(), 'USER', 'Khach hang dat ve', NOW(), NOW())
ON CONFLICT (name) DO UPDATE SET
    description = EXCLUDED.description,
    updated_at = NOW();

-- =========================================
-- 3. ROLE - PERMISSION MAPPING
-- =========================================
DELETE FROM roles_permissions
WHERE role_id IN (
    SELECT id FROM roles WHERE name IN ('ADMIN', 'STAFF', 'USER')
);

-- ADMIN co toan bo quyen.
INSERT INTO roles_permissions (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW()
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'ADMIN';

-- STAFF: van hanh rap, quan ly lich chieu, xem booking/payment, check-in QR.
-- Khong cap quyen thanh toan/dat ve ca nhan neu chua co luong ban ve tai quay.
INSERT INTO roles_permissions (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW()
FROM roles r
JOIN permissions p ON p.name IN (
    'MOVIE_VIEW',
    'CINEMA_VIEW',
    'ROOM_VIEW',
    'ROOM_UPDATE',
    'SEAT_VIEW',
    'SEAT_UPDATE',
    'SHOWTIME_VIEW',
    'SHOWTIME_CREATE',
    'SHOWTIME_UPDATE',
    'BOOKING_VIEW_ALL',
    'BOOKING_UPDATE_STATUS',
    'PAYMENT_VIEW_ALL',
    'PROMOTION_VIEW',
    'TICKET_VIEW_ALL',
    'TICKET_CHECKIN',
    'DASHBOARD_VIEW',
    'REPORT_VIEW',
    'ANALYTICS_VIEW'
)
WHERE r.name = 'STAFF';

-- USER: xem phim, chon ghe, dat ve, thanh toan, xem ve va cap nhat ho so.
INSERT INTO roles_permissions (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW()
FROM roles r
JOIN permissions p ON p.name IN (
    'MOVIE_VIEW',
    'CINEMA_VIEW',
    'SHOWTIME_VIEW',
    'SEAT_VIEW',
    'BOOKING_CREATE',
    'BOOKING_VIEW_OWN',
    'BOOKING_CANCEL_OWN',
    'PAYMENT_CREATE',
    'PAYMENT_VIEW_OWN',
    'PROMOTION_VIEW',
    'PROFILE_UPDATE',
    'TICKET_VIEW_OWN',
    'AUTH_LOGIN',
    'AUTH_LOGOUT',
    'AUTH_REFRESH_TOKEN'
)
WHERE r.name = 'USER';

-- =========================================
-- 4. CHECK NHANH
-- =========================================
SELECT
    r.name AS role_name,
    count(rp.permission_id) AS permission_count
FROM roles r
LEFT JOIN roles_permissions rp ON rp.role_id = r.id
WHERE r.name IN ('ADMIN', 'STAFF', 'USER')
GROUP BY r.name
ORDER BY r.name;
