-- ========================================================
-- MOCK DATA CINEMA BOOKING SYSTEM (BẢN ĐẦY ĐỦ)
-- Chạy script này trong pgAdmin hoặc DBeaver (Database: cinema_db)
-- Hỗ trợ tạo tự động HÀNG NGÀN ghế và suất chiếu
-- ========================================================

-- 1. Xóa dữ liệu cũ (Cascading để không bị lỗi FK)
TRUNCATE TABLE seat_status CASCADE;
TRUNCATE TABLE showtimes CASCADE;
TRUNCATE TABLE seats CASCADE;
TRUNCATE TABLE rooms CASCADE;
TRUNCATE TABLE cinemas CASCADE;
TRUNCATE TABLE movies CASCADE;

-- 2. Khởi tạo Phim (Movies)
INSERT INTO movies (id, title, description, duration, genre, release_date, poster_url, trailer_url, status, age_rating, rating_imdb, created_at, is_deleted)
VALUES 
(gen_random_uuid(), 'Lật Mặt 7: Một Điều Ước', 'Một vé đi tuổi thơ đầy cảm xúc của Lý Hải.', 138, 'Tâm lý, Gia đình', '2024-04-26', 'https://image.tmdb.org/t/p/w500/h2mD7e8c6R3wzR9gBvH3qV3r4Z2.jpg', 'https://youtube.com', 'NOW_SHOWING', 'P', 8.5, NOW(), false),
(gen_random_uuid(), 'Deadpool & Wolverine', 'Sự trở lại của dị nhân lầy lội nhất Marvel.', 135, 'Hành động, Hài', '2024-07-26', 'https://image.tmdb.org/t/p/w500/8cdWjvZQUExUUTzyp4t6EDMubfO.jpg', 'https://youtube.com', 'NOW_SHOWING', 'C18', 9.0, NOW(), false),
(gen_random_uuid(), 'Kẻ Trộm Mặt Trăng 4', 'Gru và gia đình minion tái xuất.', 95, 'Hoạt hình', '2024-07-05', 'https://image.tmdb.org/t/p/w500/wWba3TaojhK7NlGUJIHOvOBKak7.jpg', 'https://youtube.com', 'NOW_SHOWING', 'P', 7.5, NOW(), false),
(gen_random_uuid(), 'Mai', 'Một bộ phim tâm lý tình cảm của Trấn Thành.', 131, 'Tâm lý, Tình cảm', '2024-02-10', 'https://image.tmdb.org/t/p/w500/m2E9Q8D4e2fN2hP8tW0kX3vL3S7.jpg', 'https://youtube.com', 'NOW_SHOWING', 'C18', 8.0, NOW(), false),
(gen_random_uuid(), 'Dune: Part Two', 'Hành trình báo thù của Paul Atreides.', 166, 'Khoa học viễn tưởng', '2024-03-01', 'https://image.tmdb.org/t/p/w500/1pdfLvkbY9ohJlCjQH2JGjjc9CW.jpg', 'https://youtube.com', 'NOW_SHOWING', 'C13', 8.8, NOW(), false),
(gen_random_uuid(), 'Avatar: Fire and Ash', 'Phần 3 của siêu phẩm Avatar tỷ đô.', 190, 'Viễn tưởng, Hành động', '2025-12-19', 'https://image.tmdb.org/t/p/w500/t6HIqrNDIGPvXJvvqTeeK1OAS0g.jpg', 'https://youtube.com', 'COMING_SOON', 'C13', 0.0, NOW(), false),
(gen_random_uuid(), 'Venom: The Last Dance', 'Trận chiến cuối cùng của Venom.', 120, 'Hành động, Kinh dị', '2024-10-25', 'https://image.tmdb.org/t/p/w500/A31Vp1w33x28cO5r7DpxmZpE2C8.jpg', 'https://youtube.com', 'COMING_SOON', 'C16', 0.0, NOW(), false);

-- 3. Khởi tạo Rạp chiếu (Cinemas) trải dài 4 thành phố
INSERT INTO cinemas (id, name, address, city, latitude, longitude, created_at, is_active, is_deleted)
VALUES 
-- TP.HCM
(gen_random_uuid(), 'CGV Sư Vạn Hạnh', 'Tầng 6 Vạn Hạnh Mall, Quận 10', 'TP Hồ Chí Minh', 10.7715, 106.6685, NOW(), true, false),
(gen_random_uuid(), 'BHD Star Bitexco', 'Tầng 3 Bitexco, Quận 1', 'TP Hồ Chí Minh', 10.7719, 106.7044, NOW(), true, false),
-- Hà Nội
(gen_random_uuid(), 'Lotte Cinema Landmark', 'Keangnam Landmark, Phạm Hùng', 'Hà Nội', 21.0169, 105.7865, NOW(), true, false),
(gen_random_uuid(), 'CGV Vincom Bà Triệu', 'Tầng 6 Vincom Bà Triệu, Hai Bà Trưng', 'Hà Nội', 21.0116, 105.8497, NOW(), true, false),
-- Đà Nẵng
(gen_random_uuid(), 'Galaxy Đà Nẵng', 'Siêu thị Co.opmart, 478 Điện Biên Phủ', 'Đà Nẵng', 16.0645, 108.1884, NOW(), true, false),
-- Huế
(gen_random_uuid(), 'Cinestar Huế', '25 Hai Bà Trưng, Vĩnh Ninh', 'Huế', 16.4637, 107.5905, NOW(), true, false);

-- 4. Khởi tạo Phòng chiếu (Rooms) (Mỗi rạp 2-3 phòng)
INSERT INTO rooms (id, cinema_id, name, created_at, is_deleted)
SELECT gen_random_uuid(), id, 'RAP ' || room_idx, NOW(), false
FROM cinemas
CROSS JOIN generate_series(1, 2) AS room_idx; -- Sinh tự động mỗi rạp 2 phòng

-- 5. Tạo Ghế (Seats) tự động bằng generate_series của PostgreSQL
-- Mỗi phòng sẽ có 8 hàng (A-H), mỗi hàng 12 ghế. (Tổng: 96 ghế/phòng)
-- Hàng A-E (0-4): NORMAL, Hàng F-H (5-7): VIP
INSERT INTO seats (id, room_id, row_label, seat_number, row_index, col_index, seat_type, price_multiplier, created_at, is_deleted)
SELECT 
    gen_random_uuid(),
    r.id,
    chr(65 + r_idx), -- Tạo chữ A, B, C... (65 là ASCII của 'A')
    c_idx + 1,       -- Số ghế 1, 2, 3...
    r_idx,
    c_idx,
    CASE WHEN r_idx < 5 THEN 'NORMAL' ELSE 'VIP' END,
    CASE WHEN r_idx < 5 THEN 1.0 ELSE 1.5 END,
    NOW(),
    false
FROM rooms r
CROSS JOIN generate_series(0, 7) AS r_idx   -- 8 hàng
CROSS JOIN generate_series(0, 11) AS c_idx; -- 12 cột

-- 6. Khởi tạo Suất chiếu (Showtimes)
-- Mỗi ngày trong 3 ngày tới, sẽ chiếu ngẫu nhiên phim NOW_SHOWING tại tất cả các rạp (Giờ chiếu 18:00)
INSERT INTO showtimes (id, movie_id, room_id, start_time, end_time, base_price, status, created_at, is_deleted)
SELECT 
    gen_random_uuid(),
    m.id,
    r.id,
    CURRENT_DATE + (day_offset || ' days')::interval + '18:00:00'::interval,
    CURRENT_DATE + (day_offset || ' days')::interval + '18:00:00'::interval + (m.duration || ' minutes')::interval,
    80000, -- Giá cơ bản 80k
    'UPCOMING',
    NOW(),
    false
FROM (SELECT * FROM movies WHERE status = 'NOW_SHOWING' LIMIT 3) m
CROSS JOIN rooms r
CROSS JOIN generate_series(1, 3) AS day_offset;

-- 7. Khởi tạo Trạng thái ghế (SeatStatus) cho tất cả suất chiếu
-- Tự động map tất cả ghế vào suất chiếu với trạng thái AVAILABLE
INSERT INTO seat_status (id, showtime_id, seat_id, status, version)
SELECT 
    gen_random_uuid(),
    st.id,
    s.id,
    'AVAILABLE',
    0
FROM showtimes st
JOIN seats s ON s.room_id = st.room_id;

-- Xong! Dữ liệu đã được nạp thành công.
