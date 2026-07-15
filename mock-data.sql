-- ========================================================
-- MOCK DATA CINEMA BOOKING SYSTEM
-- Chỉ seed dữ liệu mẫu, không tạo bảng và không seed lại RBAC core.
-- RBAC (permissions, roles, roles_permissions, admin) do ApplicationInitConfig quản lý.
-- ========================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- 1. DỌN DẸP DỮ LIỆU NGHIỆP VỤ CŨ
-- Không truncate users/roles/permissions để tránh xoá dữ liệu do ApplicationInitConfig tạo.
TRUNCATE TABLE
    tickets,
    payments,
    booking_details,
    bookings,
    seat_status,
    promotions,
    showtimes,
    seats,
    rooms,
    cinemas,
    movies
RESTART IDENTITY CASCADE;

-- =========================================
-- 2. USERS TEST
-- =========================================
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ADMIN')
       OR NOT EXISTS (SELECT 1 FROM roles WHERE name = 'STAFF')
       OR NOT EXISTS (SELECT 1 FROM roles WHERE name = 'USER') THEN
        RAISE EXCEPTION 'Missing roles ADMIN/STAFF/USER. Start the Spring Boot app once so ApplicationInitConfig can seed RBAC first.';
    END IF;
END $$;

-- Mật khẩu mặc định: 123456 (BCrypt)
INSERT INTO users (
    id, username, password, first_name, last_name, email,
    created_at, updated_at, is_active, is_deleted
) VALUES
(uuid_generate_v4(), 'staff1', '$2a$12$R9h/cIPz0gi.URNNX3ch2e7vtPqUoXU/B6sO4m6m0/Y/YtWfC.7s6', 'Nhân', 'Viên', 'staff@cinema.com', NOW(), NOW(), true, false),
(uuid_generate_v4(), 'user1',  '$2a$12$R9h/cIPz0gi.URNNX3ch2e7vtPqUoXU/B6sO4m6m0/Y/YtWfC.7s6', 'Khách', 'Hàng 1', 'user1@cinema.com', NOW(), NOW(), true, false),
(uuid_generate_v4(), 'user2',  '$2a$12$R9h/cIPz0gi.URNNX3ch2e7vtPqUoXU/B6sO4m6m0/Y/YtWfC.7s6', 'Khách', 'Hàng 2', 'user2@cinema.com', NOW(), NOW(), true, false)
ON CONFLICT (username) DO UPDATE SET
    password = EXCLUDED.password,
    first_name = EXCLUDED.first_name,
    last_name = EXCLUDED.last_name,
    email = EXCLUDED.email,
    updated_at = NOW(),
    is_active = true,
    is_deleted = false;

DELETE FROM users_roles ur
USING users u
WHERE ur.user_id = u.id
  AND u.username IN ('staff1', 'user1', 'user2');

INSERT INTO users_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.name = 'STAFF'
WHERE u.username = 'staff1';

INSERT INTO users_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.name = 'USER'
WHERE u.username IN ('user1', 'user2');

-- =========================================
-- 3. MOVIES
-- =========================================
-- Tất cả phim mẫu đều NOW_SHOWING để phim nào cũng có suất chiếu.
INSERT INTO movies (
    id, title, description, duration, genre, release_date, poster_url,
    status, director, actors, language, subtitle_language, country,
    age_rating, trailer_url, rating_imdb, created_at, updated_at, is_deleted
) VALUES
(uuid_generate_v4(), 'Lật Mặt 7: Một Điều Ước', 'Câu chuyện gia đình nhiều cảm xúc về tình thân và những lựa chọn khó nói.', 138, 'Tâm lý, Gia đình', '2024-04-26', 'https://image.tmdb.org/t/p/w500/h2mD7e8c6R3wzR9gBvH3qV3r4Z2.jpg', 'NOW_SHOWING', 'Lý Hải', 'Thanh Hiền, Trương Minh Cường, Đinh Y Nhung', 'Tiếng Việt', 'Không', 'Việt Nam', 'P', 'https://youtube.com', 7.8, NOW(), NOW(), false),
(uuid_generate_v4(), 'Deadpool & Wolverine', 'Bộ đôi dị nhân bước vào một nhiệm vụ hỗn loạn, hài hước và đầy hành động.', 128, 'Hành động, Hài', '2024-07-26', 'https://image.tmdb.org/t/p/w500/8cdWjvZQUExUUTzyp4t6EDMubfO.jpg', 'NOW_SHOWING', 'Shawn Levy', 'Ryan Reynolds, Hugh Jackman, Emma Corrin', 'Tiếng Anh', 'Phụ đề Việt', 'Mỹ', 'C18', 'https://youtube.com', 7.6, NOW(), NOW(), false),
(uuid_generate_v4(), 'Kẻ Trộm Mặt Trăng 4', 'Gru và gia đình bước vào cuộc phiêu lưu mới với những rắc rối vui nhộn.', 95, 'Hoạt hình, Hài', '2024-07-05', 'https://image.tmdb.org/t/p/w500/wWba3TaojhK7NlGUJIHOvOBKak7.jpg', 'NOW_SHOWING', 'Chris Renaud', 'Steve Carell, Kristen Wiig, Will Ferrell', 'Lồng tiếng Việt', 'Không', 'Mỹ', 'P', 'https://youtube.com', 6.4, NOW(), NOW(), false),
(uuid_generate_v4(), 'Dune: Part Two', 'Paul Atreides tiếp tục hành trình trên Arrakis giữa định mệnh, quyền lực và báo thù.', 166, 'Khoa học viễn tưởng, Phiêu lưu', '2024-03-01', 'https://image.tmdb.org/t/p/w500/1pdfLvkbY9ohJlCjQH2JGjjc9CW.jpg', 'NOW_SHOWING', 'Denis Villeneuve', 'Timothée Chalamet, Zendaya, Rebecca Ferguson', 'Tiếng Anh', 'Phụ đề Việt', 'Mỹ', 'C13', 'https://youtube.com', 8.5, NOW(), NOW(), false),
(uuid_generate_v4(), 'Mai', 'Một người phụ nữ nhiều tổn thương đi tìm cơ hội yêu thương và làm lại cuộc đời.', 131, 'Tâm lý, Tình cảm', '2024-02-10', 'https://image.tmdb.org/t/p/w500/m2E9Q8D4e2fN2hP8tW0kX3vL3S7.jpg', 'NOW_SHOWING', 'Trấn Thành', 'Phương Anh Đào, Tuấn Trần, Trấn Thành', 'Tiếng Việt', 'Không', 'Việt Nam', 'C18', 'https://youtube.com', 7.2, NOW(), NOW(), false),
(uuid_generate_v4(), 'Inside Out 2', 'Riley trưởng thành cùng những cảm xúc mới xuất hiện trong tâm trí.', 96, 'Hoạt hình, Gia đình', '2024-06-14', 'https://image.tmdb.org/t/p/w500/vpnVM9B6NMmQpWeZvzLvDESb2QY.jpg', 'NOW_SHOWING', 'Kelsey Mann', 'Amy Poehler, Maya Hawke, Kensington Tallman', 'Lồng tiếng Việt', 'Không', 'Mỹ', 'P', 'https://youtube.com', 7.7, NOW(), NOW(), false),
(uuid_generate_v4(), 'A Quiet Place: Day One', 'Ngày đầu tiên của thảm họa khi thế giới phải học cách sống trong im lặng.', 99, 'Kinh dị, Giật gân', '2024-06-28', 'https://image.tmdb.org/t/p/w500/yrpPYKijwdMHyTGIOd1iK1h0Xno.jpg', 'NOW_SHOWING', 'Michael Sarnoski', 'Lupita Nyongo, Joseph Quinn, Alex Wolff', 'Tiếng Anh', 'Phụ đề Việt', 'Mỹ', 'C16', 'https://youtube.com', 6.7, NOW(), NOW(), false),
(uuid_generate_v4(), 'Twisters', 'Một nhóm săn bão đối đầu các siêu lốc xoáy nguy hiểm tại miền trung nước Mỹ.', 122, 'Hành động, Phiêu lưu', '2024-07-19', 'https://image.tmdb.org/t/p/w500/pjnD08FlMAIXsfOLKQbvmO0f0MD.jpg', 'NOW_SHOWING', 'Lee Isaac Chung', 'Daisy Edgar-Jones, Glen Powell, Anthony Ramos', 'Tiếng Anh', 'Phụ đề Việt', 'Mỹ', 'C13', 'https://youtube.com', 6.9, NOW(), NOW(), false),
(uuid_generate_v4(), 'Conan: Ngôi Sao 5 Cánh 1 Triệu Đô', 'Conan và Hattori Heiji lần theo bí mật xoay quanh một thanh kiếm và kho báu thất lạc.', 111, 'Hoạt hình, Trinh thám', '2024-08-02', 'https://image.tmdb.org/t/p/w500/8p15sLZc7A1QHqk3F6W87LS2pY.jpg', 'NOW_SHOWING', 'Chika Nagaoka', 'Minami Takayama, Kappei Yamaguchi, Rikiya Koyama', 'Lồng tiếng Việt', 'Phụ đề Việt', 'Nhật Bản', 'C13', 'https://youtube.com', 7.1, NOW(), NOW(), false),
(uuid_generate_v4(), 'Móng Vuốt', 'Một chuyến dã ngoại biến thành cuộc chiến sinh tồn khi nhóm bạn bị săn đuổi trong rừng sâu.', 96, 'Kinh dị, Sinh tồn', '2024-06-07', 'https://image.tmdb.org/t/p/w500/zqV8MGXfpLZiFVObLxpAI7wWonJ.jpg', 'NOW_SHOWING', 'Lê Thanh Sơn', 'Tuấn Trần, Thảo Tâm, Quốc Khánh', 'Tiếng Việt', 'Không', 'Việt Nam', 'C18', 'https://youtube.com', 6.2, NOW(), NOW(), false);

-- =========================================
-- 4. CINEMAS, ROOMS, SEATS
-- =========================================
INSERT INTO cinemas (
    id, name, address, city, latitude, longitude,
    created_at, updated_at, is_active, is_deleted
) VALUES
(uuid_generate_v4(), 'CGV Sư Vạn Hạnh', 'Tầng 6 Vạn Hạnh Mall, Quận 10', 'TP Hồ Chí Minh', 10.7715, 106.6685, NOW(), NOW(), true, false),
(uuid_generate_v4(), 'BHD Star Bitexco', 'Tầng 3 Bitexco, Quận 1', 'TP Hồ Chí Minh', 10.7719, 106.7044, NOW(), NOW(), true, false),
(uuid_generate_v4(), 'Lotte Cinema Landmark', 'Keangnam Landmark, Phạm Hùng', 'Hà Nội', 21.0169, 105.7865, NOW(), NOW(), true, false),
(uuid_generate_v4(), 'Cinestar Huế', '25 Hai Bà Trưng, Phường Vĩnh Ninh', 'Huế', 16.4621, 107.5909, NOW(), NOW(), true, false),
(uuid_generate_v4(), 'Lotte Cinema Huế', 'Tầng 4 Big C Huế, 181 Bà Triệu', 'Huế', 16.4637, 107.5949, NOW(), NOW(), true, false),
(uuid_generate_v4(), 'CGV Vincom Đà Nẵng', 'Tầng 4 Vincom Plaza, 910A Ngô Quyền', 'Đà Nẵng', 16.0711, 108.2294, NOW(), NOW(), true, false),
(uuid_generate_v4(), 'Galaxy Đà Nẵng', '478 Điện Biên Phủ, Quận Thanh Khê', 'Đà Nẵng', 16.0677, 108.1948, NOW(), NOW(), true, false),
(uuid_generate_v4(), 'Lotte Cinema Đà Nẵng', 'Tầng 5 Lotte Mart, 6 Nại Nam', 'Đà Nẵng', 16.0392, 108.2265, NOW(), NOW(), true, false);

-- Mỗi rạp 2 phòng. Tổng: 16 phòng.
INSERT INTO rooms (id, cinema_id, name, created_at, updated_at, is_deleted)
SELECT uuid_generate_v4(), c.id, 'RAP ' || room_idx, NOW(), NOW(), false
FROM cinemas c
CROSS JOIN generate_series(1, 2) AS room_idx;

-- Mỗi phòng 8 hàng x 12 ghế = 96 ghế.
-- A-E: NORMAL, F-G: VIP, H: COUPLE.
INSERT INTO seats (
    id, room_id, row_label, seat_number, row_index, col_index,
    seat_type, price_multiplier, created_at, updated_at, is_deleted
)
SELECT
    uuid_generate_v4(),
    r.id,
    chr(65 + row_idx),
    col_idx + 1,
    row_idx,
    col_idx,
    CASE
        WHEN row_idx = 7 THEN 'COUPLE'
        WHEN row_idx >= 5 THEN 'VIP'
        ELSE 'NORMAL'
    END,
    CASE
        WHEN row_idx = 7 THEN 1.8
        WHEN row_idx >= 5 THEN 1.5
        ELSE 1.0
    END,
    NOW(),
    NOW(),
    false
FROM rooms r
CROSS JOIN generate_series(0, 7) AS row_idx
CROSS JOIN generate_series(0, 11) AS col_idx;

-- =========================================
-- 5. PROMOTIONS
-- =========================================
INSERT INTO promotions (
    id, code, description, discount_type, discount_value, max_discount_amount,
    min_order_value, start_date, end_date, usage_limit, used_count,
    is_active, is_deleted, created_at, updated_at
) VALUES
(uuid_generate_v4(), 'WELCOME10', 'Giảm 10% cho đơn đầu tiên', 'PERCENT', 10, 50000, 100000, NOW() - INTERVAL '7 days', NOW() + INTERVAL '60 days', 500, 0, true, false, NOW(), NOW()),
(uuid_generate_v4(), 'CINEMA50K', 'Giảm trực tiếp 50.000đ cho đơn từ 200.000đ', 'FIXED', 50000, NULL, 200000, NOW() - INTERVAL '7 days', NOW() + INTERVAL '45 days', 300, 0, true, false, NOW(), NOW()),
(uuid_generate_v4(), 'STUDENT20', 'Giảm 20% cho học sinh sinh viên', 'PERCENT', 20, 60000, 120000, NOW() - INTERVAL '3 days', NOW() + INTERVAL '30 days', 250, 0, true, false, NOW(), NOW()),
(uuid_generate_v4(), 'WEEKDAY30K', 'Giảm 30.000đ cho suất chiếu ngày thường', 'FIXED', 30000, NULL, 150000, NOW() - INTERVAL '3 days', NOW() + INTERVAL '40 days', 400, 0, true, false, NOW(), NOW());

-- =========================================
-- 6. SHOWTIMES
-- =========================================
-- Sinh 4 khung giờ mỗi ngày trong 5 ngày tới cho tất cả phòng.
-- Movie được xoay vòng theo phòng/ngày/slot để mọi phim NOW_SHOWING đều có nhiều suất.
WITH movie_pool AS (
    SELECT
        id,
        duration,
        row_number() OVER (ORDER BY title) AS movie_no,
        count(*) OVER () AS movie_count
    FROM movies
    WHERE status = 'NOW_SHOWING'
      AND is_deleted = false
),
room_pool AS (
    SELECT
        r.id,
        row_number() OVER (ORDER BY c.name, r.name) AS room_no
    FROM rooms r
    JOIN cinemas c ON c.id = r.cinema_id
    WHERE r.is_deleted = false
      AND c.is_deleted = false
      AND c.is_active = true
),
days AS (
    SELECT generate_series(1, 5) AS day_offset
),
time_slots AS (
    SELECT * FROM (VALUES
        (1, TIME '09:00:00', 70000::numeric),
        (2, TIME '12:30:00', 80000::numeric),
        (3, TIME '16:00:00', 90000::numeric),
        (4, TIME '19:30:00', 110000::numeric)
    ) AS t(slot_no, start_at, base_price)
)
INSERT INTO showtimes (
    id, movie_id, room_id, start_time, end_time,
    base_price, status, created_at, updated_at, is_deleted
)
SELECT
    uuid_generate_v4(),
    m.id,
    r.id,
    CURRENT_DATE + d.day_offset + ts.start_at,
    CURRENT_DATE + d.day_offset + ts.start_at + (m.duration || ' minutes')::interval,
    ts.base_price,
    'UPCOMING',
    NOW(),
    NOW(),
    false
FROM room_pool r
CROSS JOIN days d
CROSS JOIN time_slots ts
JOIN movie_pool m
  ON m.movie_no = (((r.room_no + d.day_offset + ts.slot_no - 3) % m.movie_count) + 1);

-- =========================================
-- 7. SEAT STATUS
-- =========================================
-- Invariant quan trọng: mỗi showtime phải có đủ seat_status cho toàn bộ ghế của phòng đó.
INSERT INTO seat_status (
    id, showtime_id, seat_id, status, version, created_at, updated_at
)
SELECT
    uuid_generate_v4(),
    st.id,
    s.id,
    'AVAILABLE',
    0,
    NOW(),
    NOW()
FROM showtimes st
JOIN seats s ON s.room_id = st.room_id
WHERE st.is_deleted = false
  AND s.is_deleted = false;

-- =========================================
-- 8. QUICK TEST DATA: BOOKING + PAYMENT + TICKET
-- =========================================
-- Tạo 1 suất chiếu bắt đầu sau 30 phút, nằm trong cửa sổ check-in 60 phút.
-- QR ticket được ký bằng JWT_SECRET hiện tại trong .env. Nếu đổi JWT_SECRET/TICKET_QR_SECRET,
-- hãy cập nhật demo_qr_secret bên dưới cho khớp để staff scan QR không bị INVALID_QR_CODE.
WITH constants AS (
    SELECT
        '00000000-0000-0000-0000-000000000901'::uuid AS demo_showtime_id,
        '00000000-0000-0000-0000-000000000902'::uuid AS demo_booking_id,
        '00000000-0000-0000-0000-000000000903'::uuid AS demo_detail_1_id,
        '00000000-0000-0000-0000-000000000904'::uuid AS demo_detail_2_id,
        '00000000-0000-0000-0000-000000000905'::uuid AS demo_payment_id,
        '00000000-0000-0000-0000-000000000906'::uuid AS demo_ticket_1_id,
        '00000000-0000-0000-0000-000000000907'::uuid AS demo_ticket_2_id,
        'AAAAAAAAAAAAAAAAAAAAAA'::text AS demo_nonce,
        'eM4ritLt9dfucTMfTTTX5afCVKnWD1OH6YTOrO6RnMyDIBkLk4al8oZUKTBnIKvwthu5TbEROewKIoYsktEAa3'::text AS demo_qr_secret
),
demo_movie AS (
    SELECT id, duration
    FROM movies
    WHERE title = 'Lật Mặt 7: Một Điều Ước'
    LIMIT 1
),
demo_room AS (
    SELECT r.id
    FROM rooms r
    JOIN cinemas c ON c.id = r.cinema_id
    WHERE c.name = 'CGV Sư Vạn Hạnh'
      AND r.name = 'RAP 1'
    LIMIT 1
),
inserted_showtime AS (
    INSERT INTO showtimes (
        id, movie_id, room_id, start_time, end_time,
        base_price, status, created_at, updated_at, is_deleted
    )
    SELECT
        c.demo_showtime_id,
        m.id,
        r.id,
        NOW() + INTERVAL '30 minutes',
        NOW() + INTERVAL '3 hours',
        70000,
        'UPCOMING',
        NOW(),
        NOW(),
        false
    FROM constants c
    CROSS JOIN demo_movie m
    CROSS JOIN demo_room r
    RETURNING id, room_id, base_price
),
inserted_demo_seat_status AS (
    INSERT INTO seat_status (
        id, showtime_id, seat_id, status, version, created_at, updated_at
    )
    SELECT
        uuid_generate_v4(),
        st.id,
        s.id,
        CASE
            WHEN s.row_label = 'A' AND s.seat_number IN (1, 2) THEN 'BOOKED'
            ELSE 'AVAILABLE'
        END,
        0,
        NOW(),
        NOW()
    FROM inserted_showtime st
    JOIN seats s ON s.room_id = st.room_id
    WHERE s.is_deleted = false
    RETURNING seat_id
),
demo_user AS (
    SELECT id
    FROM users
    WHERE username = 'user1'
    LIMIT 1
),
inserted_booking AS (
    INSERT INTO bookings (
        id, user_id, showtime_id, promotion_id, total_price,
        discount_amount, status, secure_token, created_at, updated_at
    )
    SELECT
        c.demo_booking_id,
        u.id,
        st.id,
        NULL,
        st.base_price * 2,
        0,
        'SUCCESS',
        'demo-success-booking-token',
        NOW(),
        NOW()
    FROM constants c
    CROSS JOIN demo_user u
    CROSS JOIN inserted_showtime st
    RETURNING id
),
selected_demo_seats AS (
    SELECT
        s.id,
        row_number() OVER (ORDER BY s.seat_number) AS seat_no
    FROM inserted_showtime st
    JOIN seats s ON s.room_id = st.room_id
    WHERE s.row_label = 'A'
      AND s.seat_number IN (1, 2)
),
inserted_booking_details AS (
    INSERT INTO booking_details (
        id, booking_id, seat_id, price_at_booking, created_at, updated_at
    )
    SELECT
        CASE WHEN s.seat_no = 1 THEN c.demo_detail_1_id ELSE c.demo_detail_2_id END,
        b.id,
        s.id,
        70000,
        NOW(),
        NOW()
    FROM selected_demo_seats s
    CROSS JOIN constants c
    CROSS JOIN inserted_booking b
    RETURNING id
),
inserted_payment AS (
    INSERT INTO payments (
        id, booking_id, amount, method, transaction_no,
        status, provider_response, payment_time, created_at, updated_at
    )
    SELECT
        c.demo_payment_id,
        b.id,
        140000,
        'VNPAY',
        'DEMO_VNPAY_SUCCESS_001',
        'SUCCESS',
        '{"demo": true, "message": "Seeded payment for quick check-in test"}'::jsonb,
        NOW(),
        NOW(),
        NOW()
    FROM constants c
    CROSS JOIN inserted_booking b
    RETURNING id
),
ticket_payloads AS (
    SELECT
        bd.id AS booking_detail_id,
        CASE
            WHEN bd.id = c.demo_detail_1_id THEN c.demo_ticket_1_id
            ELSE c.demo_ticket_2_id
        END AS ticket_id,
        'CBT1.' || replace(upper(bd.id::text), '-', '') || '.' || c.demo_nonce AS payload,
        c.demo_qr_secret
    FROM inserted_booking_details bd
    CROSS JOIN constants c
),
inserted_tickets AS (
    INSERT INTO tickets (
        id, booking_detail_id, qr_code, status, check_in_time, created_at, updated_at
    )
    SELECT
        p.ticket_id,
        p.booking_detail_id,
        p.payload || '.' || translate(
            rtrim(encode(substring(hmac(p.payload, p.demo_qr_secret, 'sha256') from 1 for 24), 'base64'), '='),
            '+/',
            '-_'
        ),
        'ACTIVE',
        NULL,
        NOW(),
        NOW()
    FROM ticket_payloads p
    RETURNING qr_code
)
SELECT 'Quick test ticket QR: ' || qr_code AS demo_ticket_qr
FROM inserted_tickets;

-- =========================================
-- 9. KIỂM TRA DỮ LIỆU SAU KHI SEED
-- =========================================
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM movies m
        WHERE m.status = 'NOW_SHOWING'
          AND m.is_deleted = false
          AND NOT EXISTS (
              SELECT 1
              FROM showtimes st
              WHERE st.movie_id = m.id
                AND st.is_deleted = false
          )
    ) THEN
        RAISE EXCEPTION 'Mock data invalid: at least one NOW_SHOWING movie has no showtime.';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM showtimes st
        WHERE st.is_deleted = false
          AND (
              SELECT count(*)
              FROM seats s
              WHERE s.room_id = st.room_id
                AND s.is_deleted = false
          ) <> (
              SELECT count(*)
              FROM seat_status ss
              WHERE ss.showtime_id = st.id
          )
    ) THEN
        RAISE EXCEPTION 'Mock data invalid: at least one showtime does not have a complete seat map.';
    END IF;
END $$;

-- Tài khoản test:
-- admin: tạo bởi ApplicationInitConfig, mật khẩu mặc định lấy từ app.admin.default-password hoặc admin123.
-- staff1 / user1 / user2: mật khẩu 123456.
-- Kỳ vọng dữ liệu:
-- 10 phim NOW_SHOWING, 8 rạp, 16 phòng, 1536 ghế, 321 suất chiếu, 30816 dòng seat_status.
-- Vé test nhanh:
-- user1 có booking SUCCESS tại CGV Sư Vạn Hạnh, RAP 1, ghế A1/A2, suất chiếu bắt đầu sau 30 phút.
-- Lấy QR để staff check-in:
-- SELECT t.qr_code
-- FROM tickets t
-- JOIN booking_details bd ON bd.id = t.booking_detail_id
-- JOIN bookings b ON b.id = bd.booking_id
-- WHERE b.secure_token = 'demo-success-booking-token';
