-- ========================================================
-- CINEMA BOOKING SYSTEM - DEMO DATA
-- ========================================================
-- DML only. Apply Flyway migrations before running this file.
-- This script resets business/demo data and seeds test accounts, cinemas,
-- rooms, seats, showtimes, promotions, bookings, payments, and tickets.
-- Never run it against a production database.
-- ========================================================

DELETE FROM refresh_tokens;
DELETE FROM auth_audit_logs;
DELETE FROM admin_audit_logs;

TRUNCATE TABLE
    tickets,
    refunds,
    payment_events,
    payments,
    booking_details,
    bookings,
    seat_status,
    promotions,
    showtimes,
    seats,
    rooms,
    staff_cinemas,
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
    id, username, password, first_name, last_name, dob, phone, email,
    avatar_url, email_verified, created_at, updated_at, is_active, is_deleted
) VALUES
(uuid_generate_v4(), 'staff1', '$2a$10$IB6dDsPRTXg.d94FjmekPe7TWIi/xAbgIT3vozfkaZ9Dj0cOobzky', 'Nhân', 'Viên', '1995-03-12', '0901000001', 'staff@cinema.com', 'https://i.pravatar.cc/160?img=12', true, NOW(), NOW(), true, false),
(uuid_generate_v4(), 'staff_hcm', '$2a$10$IB6dDsPRTXg.d94FjmekPe7TWIi/xAbgIT3vozfkaZ9Dj0cOobzky', 'Minh', 'Khang', '1994-08-21', '0901000002', 'staff.hcm@cinemabooking.vn', 'https://i.pravatar.cc/160?img=15', true, NOW(), NOW(), true, false),
(uuid_generate_v4(), 'staff_hanoi', '$2a$10$IB6dDsPRTXg.d94FjmekPe7TWIi/xAbgIT3vozfkaZ9Dj0cOobzky', 'Thu', 'Trang', '1996-01-18', '0901000003', 'staff.hanoi@cinemabooking.vn', 'https://i.pravatar.cc/160?img=32', true, NOW(), NOW(), true, false),
(uuid_generate_v4(), 'staff_danang', '$2a$10$IB6dDsPRTXg.d94FjmekPe7TWIi/xAbgIT3vozfkaZ9Dj0cOobzky', 'Hoàng', 'Nam', '1993-11-05', '0901000004', 'staff.danang@cinemabooking.vn', 'https://i.pravatar.cc/160?img=56', true, NOW(), NOW(), true, false),
(uuid_generate_v4(), 'staff_hue', '$2a$10$IB6dDsPRTXg.d94FjmekPe7TWIi/xAbgIT3vozfkaZ9Dj0cOobzky', 'Bảo', 'Ngọc', '1997-06-26', '0901000005', 'staff.hue@cinemabooking.vn', 'https://i.pravatar.cc/160?img=47', true, NOW(), NOW(), true, false),
(uuid_generate_v4(), 'staff_unassigned', '$2a$10$IB6dDsPRTXg.d94FjmekPe7TWIi/xAbgIT3vozfkaZ9Dj0cOobzky', 'Chưa', 'Phân Công', '1998-09-09', '0901000006', 'staff.unassigned@cinemabooking.vn', 'https://i.pravatar.cc/160?img=68', true, NOW(), NOW(), true, false),
(uuid_generate_v4(), 'staff_blocked', '$2a$10$IB6dDsPRTXg.d94FjmekPe7TWIi/xAbgIT3vozfkaZ9Dj0cOobzky', 'Tạm', 'Khóa', '1992-12-02', '0901000007', 'staff.blocked@cinemabooking.vn', 'https://i.pravatar.cc/160?img=20', true, NOW(), NOW(), false, false),
(uuid_generate_v4(), 'user1',  '$2a$10$IB6dDsPRTXg.d94FjmekPe7TWIi/xAbgIT3vozfkaZ9Dj0cOobzky', 'Khách', 'Hàng 1', '1999-04-10', '0912000001', 'user1@cinema.com', 'https://i.pravatar.cc/160?img=1', true, NOW(), NOW(), true, false),
(uuid_generate_v4(), 'user2',  '$2a$10$IB6dDsPRTXg.d94FjmekPe7TWIi/xAbgIT3vozfkaZ9Dj0cOobzky', 'Khách', 'Hàng 2', '2000-07-14', '0912000002', 'user2@cinema.com', 'https://i.pravatar.cc/160?img=2', true, NOW(), NOW(), true, false),
(uuid_generate_v4(), 'user3',  '$2a$10$IB6dDsPRTXg.d94FjmekPe7TWIi/xAbgIT3vozfkaZ9Dj0cOobzky', 'An', 'Nhiên', '1998-02-22', '0912000003', 'annhien@example.com', 'https://i.pravatar.cc/160?img=3', true, NOW(), NOW(), true, false),
(uuid_generate_v4(), 'user4',  '$2a$10$IB6dDsPRTXg.d94FjmekPe7TWIi/xAbgIT3vozfkaZ9Dj0cOobzky', 'Quốc', 'Bảo', '1997-10-30', '0912000004', 'quocbao@example.com', 'https://i.pravatar.cc/160?img=4', true, NOW(), NOW(), true, false),
(uuid_generate_v4(), 'user5',  '$2a$10$IB6dDsPRTXg.d94FjmekPe7TWIi/xAbgIT3vozfkaZ9Dj0cOobzky', 'Mai', 'Chi', '2001-05-19', '0912000005', 'maichi@example.com', 'https://i.pravatar.cc/160?img=5', true, NOW(), NOW(), true, false),
(uuid_generate_v4(), 'user6',  '$2a$10$IB6dDsPRTXg.d94FjmekPe7TWIi/xAbgIT3vozfkaZ9Dj0cOobzky', 'Gia', 'Huy', '1996-12-24', '0912000006', 'giahuy@example.com', 'https://i.pravatar.cc/160?img=6', true, NOW(), NOW(), true, false),
(uuid_generate_v4(), 'user7',  '$2a$10$IB6dDsPRTXg.d94FjmekPe7TWIi/xAbgIT3vozfkaZ9Dj0cOobzky', 'Thanh', 'Vy', '2002-01-08', '0912000007', 'thanhvy@example.com', 'https://i.pravatar.cc/160?img=7', true, NOW(), NOW(), true, false),
(uuid_generate_v4(), 'user8',  '$2a$10$IB6dDsPRTXg.d94FjmekPe7TWIi/xAbgIT3vozfkaZ9Dj0cOobzky', 'Đức', 'Anh', '1995-08-03', '0912000008', 'ducanh@example.com', 'https://i.pravatar.cc/160?img=8', true, NOW(), NOW(), true, false),
(uuid_generate_v4(), 'user_vip', '$2a$10$IB6dDsPRTXg.d94FjmekPe7TWIi/xAbgIT3vozfkaZ9Dj0cOobzky', 'Linh', 'Đan', '1994-11-16', '0912000009', 'vip.customer@example.com', 'https://i.pravatar.cc/160?img=9', true, NOW(), NOW(), true, false),
(uuid_generate_v4(), 'user_pending', '$2a$10$IB6dDsPRTXg.d94FjmekPe7TWIi/xAbgIT3vozfkaZ9Dj0cOobzky', 'Chờ', 'Xác Thực', '2003-03-03', '0912000010', 'pending.verify@example.com', 'https://i.pravatar.cc/160?img=10', false, NOW(), NOW(), true, false),
(uuid_generate_v4(), 'user_blocked', '$2a$10$IB6dDsPRTXg.d94FjmekPe7TWIi/xAbgIT3vozfkaZ9Dj0cOobzky', 'Người', 'Bị Khóa', '1991-09-12', '0912000011', 'blocked.user@example.com', 'https://i.pravatar.cc/160?img=11', true, NOW(), NOW(), false, false)
ON CONFLICT (username) DO UPDATE SET
    password = EXCLUDED.password,
    first_name = EXCLUDED.first_name,
    last_name = EXCLUDED.last_name,
    dob = EXCLUDED.dob,
    phone = EXCLUDED.phone,
    email = EXCLUDED.email,
    avatar_url = EXCLUDED.avatar_url,
    email_verified = EXCLUDED.email_verified,
    updated_at = NOW(),
    is_active = EXCLUDED.is_active,
    is_deleted = EXCLUDED.is_deleted;

DELETE FROM users_roles ur
USING users u
WHERE ur.user_id = u.id
  AND u.username IN (
      'staff1', 'staff_hcm', 'staff_hanoi', 'staff_danang', 'staff_hue', 'staff_unassigned', 'staff_blocked',
      'user1', 'user2', 'user3', 'user4', 'user5', 'user6', 'user7', 'user8', 'user_vip', 'user_pending', 'user_blocked'
  );

INSERT INTO users_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.name = 'STAFF'
WHERE u.username IN ('staff1', 'staff_hcm', 'staff_hanoi', 'staff_danang', 'staff_hue', 'staff_unassigned', 'staff_blocked');

INSERT INTO users_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.name = 'USER'
WHERE u.username IN ('user1', 'user2', 'user3', 'user4', 'user5', 'user6', 'user7', 'user8', 'user_vip', 'user_pending', 'user_blocked');

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
(uuid_generate_v4(), 'Địa Đạo: Mặt Trời Trong Bóng Tối', 'Những người lính và dân quân trong lòng địa đạo đối mặt hiểm nguy để bảo vệ quê hương.', 128, 'Chiến tranh, Tâm lý', '2025-04-04', 'https://image.tmdb.org/t/p/w500/z1p34vh7dEOnLDmyCrlUVLuoDzd.jpg', 'NOW_SHOWING', 'Bùi Thạc Chuyên', 'Thái Hòa, Quang Tuấn, Diễm Hằng Lamoon', 'Tiếng Việt', 'Không', 'Việt Nam', 'C13', 'https://youtube.com', 7.4, NOW(), NOW(), false),
(uuid_generate_v4(), 'Mưa Đỏ', 'Câu chuyện về tình đồng đội, lòng quả cảm và những mất mát giữa chiến trường khốc liệt.', 124, 'Chiến tranh, Lịch sử', '2025-08-22', 'https://image.tmdb.org/t/p/w500/pjnD08FlMAIXsfOLKQbvmO0f0MD.jpg', 'NOW_SHOWING', 'Đặng Thái Huyền', 'Đỗ Nhật Hoàng, Lâm Thanh Nhã, Steven Nguyễn', 'Tiếng Việt', 'Không', 'Việt Nam', 'C13', 'https://youtube.com', 7.3, NOW(), NOW(), false),
(uuid_generate_v4(), 'Kẻ Trộm Mặt Trăng 4', 'Gru và gia đình bước vào cuộc phiêu lưu mới với những rắc rối vui nhộn.', 95, 'Hoạt hình, Hài', '2024-07-05', 'https://image.tmdb.org/t/p/w500/wWba3TaojhK7NlGUJIHOvOBKak7.jpg', 'NOW_SHOWING', 'Chris Renaud', 'Steve Carell, Kristen Wiig, Will Ferrell', 'Lồng tiếng Việt', 'Không', 'Mỹ', 'P', 'https://youtube.com', 6.4, NOW(), NOW(), false),
(uuid_generate_v4(), 'Dune: Part Two', 'Paul Atreides tiếp tục hành trình trên Arrakis giữa định mệnh, quyền lực và báo thù.', 166, 'Khoa học viễn tưởng, Phiêu lưu', '2024-03-01', 'https://image.tmdb.org/t/p/w500/1pdfLvkbY9ohJlCjQH2JGjjc9CW.jpg', 'NOW_SHOWING', 'Denis Villeneuve', 'Timothée Chalamet, Zendaya, Rebecca Ferguson', 'Tiếng Anh', 'Phụ đề Việt', 'Mỹ', 'C13', 'https://youtube.com', 8.5, NOW(), NOW(), false),
(uuid_generate_v4(), 'Mai', 'Một người phụ nữ nhiều tổn thương đi tìm cơ hội yêu thương và làm lại cuộc đời.', 131, 'Tâm lý, Tình cảm', '2024-02-10', 'https://image.tmdb.org/t/p/w500/m2E9Q8D4e2fN2hP8tW0kX3vL3S7.jpg', 'NOW_SHOWING', 'Trấn Thành', 'Phương Anh Đào, Tuấn Trần, Trấn Thành', 'Tiếng Việt', 'Không', 'Việt Nam', 'C18', 'https://youtube.com', 7.2, NOW(), NOW(), false),
(uuid_generate_v4(), 'Inside Out 2', 'Riley trưởng thành cùng những cảm xúc mới xuất hiện trong tâm trí.', 96, 'Hoạt hình, Gia đình', '2024-06-14', 'https://image.tmdb.org/t/p/w500/vpnVM9B6NMmQpWeZvzLvDESb2QY.jpg', 'NOW_SHOWING', 'Kelsey Mann', 'Amy Poehler, Maya Hawke, Kensington Tallman', 'Lồng tiếng Việt', 'Không', 'Mỹ', 'P', 'https://youtube.com', 7.7, NOW(), NOW(), false),
(uuid_generate_v4(), 'A Quiet Place: Day One', 'Ngày đầu tiên của thảm họa khi thế giới phải học cách sống trong im lặng.', 99, 'Kinh dị, Giật gân', '2024-06-28', 'https://image.tmdb.org/t/p/w500/yrpPYKijwdMHyTGIOd1iK1h0Xno.jpg', 'NOW_SHOWING', 'Michael Sarnoski', 'Lupita Nyongo, Joseph Quinn, Alex Wolff', 'Tiếng Anh', 'Phụ đề Việt', 'Mỹ', 'C16', 'https://youtube.com', 6.7, NOW(), NOW(), false),
(uuid_generate_v4(), 'Twisters', 'Một nhóm săn bão đối đầu các siêu lốc xoáy nguy hiểm tại miền trung nước Mỹ.', 122, 'Hành động, Phiêu lưu', '2024-07-19', 'https://image.tmdb.org/t/p/w500/pjnD08FlMAIXsfOLKQbvmO0f0MD.jpg', 'NOW_SHOWING', 'Lee Isaac Chung', 'Daisy Edgar-Jones, Glen Powell, Anthony Ramos', 'Tiếng Anh', 'Phụ đề Việt', 'Mỹ', 'C13', 'https://youtube.com', 6.9, NOW(), NOW(), false),
(uuid_generate_v4(), 'Conan: Ngôi Sao 5 Cánh 1 Triệu Đô', 'Conan và Hattori Heiji lần theo bí mật xoay quanh một thanh kiếm và kho báu thất lạc.', 111, 'Hoạt hình, Trinh thám', '2024-08-02', 'https://image.tmdb.org/t/p/w500/8p15sLZc7A1QHqk3F6W87LS2pY.jpg', 'NOW_SHOWING', 'Chika Nagaoka', 'Minami Takayama, Kappei Yamaguchi, Rikiya Koyama', 'Lồng tiếng Việt', 'Phụ đề Việt', 'Nhật Bản', 'C13', 'https://youtube.com', 7.1, NOW(), NOW(), false),
(uuid_generate_v4(), 'Móng Vuốt', 'Một chuyến dã ngoại biến thành cuộc chiến sinh tồn khi nhóm bạn bị săn đuổi trong rừng sâu.', 96, 'Kinh dị, Sinh tồn', '2024-06-07', 'https://image.tmdb.org/t/p/w500/zqV8MGXfpLZiFVObLxpAI7wWonJ.jpg', 'NOW_SHOWING', 'Lê Thanh Sơn', 'Tuấn Trần, Thảo Tâm, Quốc Khánh', 'Tiếng Việt', 'Không', 'Việt Nam', 'C18', 'https://youtube.com', 6.2, NOW(), NOW(), false),
(uuid_generate_v4(), 'Nhà Bà Nữ', 'Một gia đình nhiều thế hệ va chạm trong tình yêu, định kiến và những bí mật khó nói.', 102, 'Tâm lý, Gia đình', '2023-01-22', 'https://image.tmdb.org/t/p/w500/vpnVM9B6NMmQpWeZvzLvDESb2QY.jpg', 'NOW_SHOWING', 'Trấn Thành', 'Lê Giang, Uyển Ân, Song Luân', 'Tiếng Việt', 'Không', 'Việt Nam', 'C13', 'https://youtube.com', 6.8, NOW(), NOW(), false),
(uuid_generate_v4(), 'Bố Già', 'Một người cha lao động bình dân cố gắng giữ gia đình trước những khác biệt thế hệ.', 128, 'Tâm lý, Hài', '2021-03-12', 'https://image.tmdb.org/t/p/w500/m2E9Q8D4e2fN2hP8tW0kX3vL3S7.jpg', 'NOW_SHOWING', 'Trấn Thành, Vũ Ngọc Đãng', 'Trấn Thành, Tuấn Trần, Ngân Chi', 'Tiếng Việt', 'Không', 'Việt Nam', 'C13', 'https://youtube.com', 7.1, NOW(), NOW(), false),
(uuid_generate_v4(), 'Godzilla x Kong: Đế Chế Mới', 'Hai titan huyền thoại hợp lực trước mối đe dọa cổ đại trỗi dậy từ lòng đất.', 115, 'Hành động, Quái vật', '2024-03-29', 'https://image.tmdb.org/t/p/w500/1pdfLvkbY9ohJlCjQH2JGjjc9CW.jpg', 'NOW_SHOWING', 'Adam Wingard', 'Rebecca Hall, Brian Tyree Henry, Dan Stevens', 'Tiếng Anh', 'Phụ đề Việt', 'Mỹ', 'C13', 'https://youtube.com', 6.4, NOW(), NOW(), false),
(uuid_generate_v4(), 'Kung Fu Panda 4', 'Po bước vào hành trình tìm người kế nhiệm và đối đầu một phản diện biến hóa khó lường.', 94, 'Hoạt hình, Phiêu lưu', '2024-03-08', 'https://image.tmdb.org/t/p/w500/wWba3TaojhK7NlGUJIHOvOBKak7.jpg', 'NOW_SHOWING', 'Mike Mitchell', 'Jack Black, Awkwafina, Viola Davis', 'Lồng tiếng Việt', 'Không', 'Mỹ', 'P', 'https://youtube.com', 6.3, NOW(), NOW(), false),
(uuid_generate_v4(), 'Furiosa: Câu Chuyện Từ Max Điên', 'Furiosa trẻ tuổi chiến đấu để sống sót và tìm đường trở về vùng đất của mình.', 148, 'Hành động, Phiêu lưu', '2024-05-24', 'https://image.tmdb.org/t/p/w500/yrpPYKijwdMHyTGIOd1iK1h0Xno.jpg', 'NOW_SHOWING', 'George Miller', 'Anya Taylor-Joy, Chris Hemsworth, Tom Burke', 'Tiếng Anh', 'Phụ đề Việt', 'Mỹ', 'C16', 'https://youtube.com', 7.5, NOW(), NOW(), false),
(uuid_generate_v4(), 'Thanh Gươm Diệt Quỷ: Đại Trụ Đặc Huấn', 'Tanjiro cùng các kiếm sĩ bước vào đợt đặc huấn khốc liệt trước trận chiến mới.', 104, 'Hoạt hình, Hành động', '2024-02-23', 'https://image.tmdb.org/t/p/w500/8p15sLZc7A1QHqk3F6W87LS2pY.jpg', 'NOW_SHOWING', 'Haruo Sotozaki', 'Natsuki Hanae, Akari Kito, Hiro Shimono', 'Tiếng Nhật', 'Phụ đề Việt', 'Nhật Bản', 'C13', 'https://youtube.com', 7.0, NOW(), NOW(), false),
(uuid_generate_v4(), 'Oppenheimer', 'Chân dung nhà khoa học đứng giữa lựa chọn đạo đức và bước ngoặt lịch sử của thế giới.', 180, 'Tiểu sử, Chính kịch', '2023-07-21', 'https://image.tmdb.org/t/p/w500/ptpr0kGAckfQkJeJIt8st5dglvd.jpg', 'NOW_SHOWING', 'Christopher Nolan', 'Cillian Murphy, Emily Blunt, Robert Downey Jr.', 'Tiếng Anh', 'Phụ đề Việt', 'Mỹ', 'C16', 'https://youtube.com', 8.3, NOW(), NOW(), false),
(uuid_generate_v4(), 'Top Gun: Maverick', 'Một phi công huyền thoại trở lại huấn luyện thế hệ mới cho nhiệm vụ gần như bất khả thi.', 131, 'Hành động, Chính kịch', '2022-05-27', 'https://image.tmdb.org/t/p/w500/62HCnUTziyWcpDaBO2i1DX17ljH.jpg', 'NOW_SHOWING', 'Joseph Kosinski', 'Tom Cruise, Miles Teller, Jennifer Connelly', 'Tiếng Anh', 'Phụ đề Việt', 'Mỹ', 'C13', 'https://youtube.com', 8.2, NOW(), NOW(), false),
(uuid_generate_v4(), 'Avatar: Dòng Chảy Của Nước', 'Gia đình Sully tìm nơi trú ẩn mới giữa đại dương Pandora và những mối nguy cũ.', 192, 'Khoa học viễn tưởng, Phiêu lưu', '2022-12-16', 'https://image.tmdb.org/t/p/w500/t6HIqrRAclMCA60NsSmeqe9RmNV.jpg', 'NOW_SHOWING', 'James Cameron', 'Sam Worthington, Zoe Saldana, Sigourney Weaver', 'Tiếng Anh', 'Phụ đề Việt', 'Mỹ', 'C13', 'https://youtube.com', 7.6, NOW(), NOW(), false),
(uuid_generate_v4(), 'Spider-Man: Across the Spider-Verse', 'Miles Morales bước qua đa vũ trụ và đối mặt với lựa chọn định nghĩa người hùng.', 140, 'Hoạt hình, Hành động', '2023-06-02', 'https://image.tmdb.org/t/p/w500/8Vt6mWEReuy4Of61Lnj5Xj704m8.jpg', 'NOW_SHOWING', 'Joaquim Dos Santos', 'Shameik Moore, Hailee Steinfeld, Oscar Isaac', 'Tiếng Anh', 'Phụ đề Việt', 'Mỹ', 'C13', 'https://youtube.com', 8.6, NOW(), NOW(), false),
(uuid_generate_v4(), 'Mission: Impossible - Dead Reckoning', 'Ethan Hunt chạy đua với một hiểm họa công nghệ có thể xoay chuyển trật tự thế giới.', 164, 'Hành động, Điệp viên', '2023-07-12', 'https://image.tmdb.org/t/p/w500/NNxYkU70HPurnNCSiCjYAmacwm.jpg', 'NOW_SHOWING', 'Christopher McQuarrie', 'Tom Cruise, Hayley Atwell, Ving Rhames', 'Tiếng Anh', 'Phụ đề Việt', 'Mỹ', 'C13', 'https://youtube.com', 7.7, NOW(), NOW(), false),
(uuid_generate_v4(), 'Elemental', 'Một câu chuyện tình cảm rực rỡ giữa hai cư dân ở thành phố của các nguyên tố.', 101, 'Hoạt hình, Gia đình', '2023-06-16', 'https://image.tmdb.org/t/p/w500/4Y1WNkd88JXmGfhtWR7dmDAo1T2.jpg', 'NOW_SHOWING', 'Peter Sohn', 'Leah Lewis, Mamoudou Athie, Ronnie del Carmen', 'Lồng tiếng Việt', 'Không', 'Mỹ', 'P', 'https://youtube.com', 7.0, NOW(), NOW(), false),
(uuid_generate_v4(), 'Ký Sinh Trùng', 'Hai gia đình ở hai tầng lớp xã hội va vào nhau trong một bi kịch đen tối và sắc lạnh.', 132, 'Tâm lý, Giật gân', '2019-05-30', 'https://image.tmdb.org/t/p/w500/7IiTTgloJzvGI1TAYymCfbfl3vT.jpg', 'ENDED', 'Bong Joon Ho', 'Song Kang-ho, Lee Sun-kyun, Cho Yeo-jeong', 'Tiếng Hàn', 'Phụ đề Việt', 'Hàn Quốc', 'C16', 'https://youtube.com', 8.5, NOW(), NOW(), false),
(uuid_generate_v4(), 'Ngày Xửa Ngày Xưa: Chuyến Tàu Ánh Sao', 'Một chuyến phiêu lưu gia đình giả tưởng đang được mở bán sớm cho mùa lễ hội.', 110, 'Gia đình, Phiêu lưu', (CURRENT_DATE + INTERVAL '35 days')::date, 'https://image.tmdb.org/t/p/w500/wWba3TaojhK7NlGUJIHOvOBKak7.jpg', 'COMING_SOON', 'Nguyễn Minh Anh', 'Dàn diễn viên trẻ CinemaBooking', 'Tiếng Việt', 'Không', 'Việt Nam', 'P', 'https://youtube.com', 7.0, NOW(), NOW(), false);

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
(uuid_generate_v4(), 'Lotte Cinema Huế', 'Tầng 4 Big C Huế, 181 Bà Triệu', 'Huế', 16.45988, 107.59928, NOW(), NOW(), true, false),
(uuid_generate_v4(), 'BHD Star Huế', 'Tầng 5 Vincom Plaza Huế, 50A Hùng Vương', 'Huế', 16.46296, 107.59426, NOW(), NOW(), true, false),
(uuid_generate_v4(), 'Galaxy AEON Mall Huế', 'Tầng 4 AEON Mall Huế, 08 Võ Nguyên Giáp, Phường An Cựu', 'Huế', 16.45450, 107.61420, NOW(), NOW(), true, false),
(uuid_generate_v4(), 'Starlight Huế', 'Tầng 3 Co.opmart Huế, 06 Trần Hưng Đạo', 'Huế', 16.47102, 107.58739, NOW(), NOW(), true, false),
(uuid_generate_v4(), 'CGV Vincom Đà Nẵng', 'Tầng 4 Vincom Plaza, 910A Ngô Quyền', 'Đà Nẵng', 16.0711, 108.2294, NOW(), NOW(), true, false),
(uuid_generate_v4(), 'Galaxy Đà Nẵng', '478 Điện Biên Phủ, Quận Thanh Khê', 'Đà Nẵng', 16.0677, 108.1948, NOW(), NOW(), true, false),
(uuid_generate_v4(), 'Lotte Cinema Đà Nẵng', 'Tầng 5 Lotte Mart, 6 Nại Nam', 'Đà Nẵng', 16.0392, 108.2265, NOW(), NOW(), true, false),
(uuid_generate_v4(), 'Galaxy Nguyễn Du', '116 Nguyễn Du, Quận 1', 'TP Hồ Chí Minh', 10.7758, 106.6910, NOW(), NOW(), true, false),
(uuid_generate_v4(), 'CGV Aeon Mall Bình Tân', 'Tầng 3 Aeon Mall Bình Tân, Quận Bình Tân', 'TP Hồ Chí Minh', 10.7422, 106.6128, NOW(), NOW(), true, false),
(uuid_generate_v4(), 'Beta Mỹ Đình', 'Tầng hầm B1 The Garden, Nam Từ Liêm', 'Hà Nội', 21.0156, 105.7798, NOW(), NOW(), true, false),
(uuid_generate_v4(), 'CGV Tràng Tiền Plaza', 'Tầng 5 Tràng Tiền Plaza, Hoàn Kiếm', 'Hà Nội', 21.0245, 105.8515, NOW(), NOW(), true, false),
(uuid_generate_v4(), 'Lotte Cinema Cần Thơ', 'Tầng 5 Sense City, Ninh Kiều', 'Cần Thơ', 10.0342, 105.7839, NOW(), NOW(), true, false),
(uuid_generate_v4(), 'CGV Nha Trang Center', 'Tầng 3 Nha Trang Center, Trần Phú', 'Nha Trang', 12.2473, 109.1955, NOW(), NOW(), true, false),
-- Cụm Quảng Nam quanh tọa độ 15.593163, 108.534505 để test nút "Gần tôi".
-- Có rạp trong 10km và ngoài 10km để kiểm tra sort/badge khoảng cách.
(uuid_generate_v4(), 'CinemaBooking Tam Kỳ Center', '02 Phan Bội Châu, TP Tam Kỳ', 'Quảng Nam', 15.5737, 108.4740, NOW(), NOW(), true, false),
(uuid_generate_v4(), 'Galaxy Tam Kỳ Square', '175 Phan Châu Trinh, TP Tam Kỳ', 'Quảng Nam', 15.5689, 108.4838, NOW(), NOW(), true, false),
(uuid_generate_v4(), 'Beta Tam Phú', 'Khu đô thị Tam Phú, TP Tam Kỳ', 'Quảng Nam', 15.5896, 108.5074, NOW(), NOW(), true, false),
(uuid_generate_v4(), 'Cinestar Tam Thăng', 'Đường Võ Chí Công, xã Tam Thăng', 'Quảng Nam', 15.5485, 108.5529, NOW(), NOW(), true, false),
(uuid_generate_v4(), 'Mega GS An Hà', 'Khu đô thị An Hà, TP Tam Kỳ', 'Quảng Nam', 15.6209, 108.5103, NOW(), NOW(), true, false),
(uuid_generate_v4(), 'Lotte Cinema Chu Lai', 'Khu kinh tế mở Chu Lai, Núi Thành', 'Quảng Nam', 15.4309, 108.7061, NOW(), NOW(), true, false);

-- Phân công staff theo rạp để test scope nhân viên, lọc theo thành phố/rạp và staff chưa gán rạp.
WITH assignments(username, cinema_name) AS (
    VALUES
        ('staff1', 'CGV Sư Vạn Hạnh'),
        ('staff1', 'BHD Star Bitexco'),
        ('staff_hcm', 'CGV Sư Vạn Hạnh'),
        ('staff_hcm', 'Galaxy Nguyễn Du'),
        ('staff_hcm', 'CGV Aeon Mall Bình Tân'),
        ('staff_hanoi', 'Lotte Cinema Landmark'),
        ('staff_hanoi', 'Beta Mỹ Đình'),
        ('staff_hanoi', 'CGV Tràng Tiền Plaza'),
        ('staff_danang', 'CGV Vincom Đà Nẵng'),
        ('staff_danang', 'Galaxy Đà Nẵng'),
        ('staff_danang', 'Lotte Cinema Đà Nẵng'),
        ('staff_danang', 'CinemaBooking Tam Kỳ Center'),
        ('staff_danang', 'Galaxy Tam Kỳ Square'),
        ('staff_danang', 'Beta Tam Phú'),
        ('staff_danang', 'Cinestar Tam Thăng'),
        ('staff_danang', 'Mega GS An Hà'),
        ('staff_danang', 'Lotte Cinema Chu Lai'),
        ('staff_hue', 'Cinestar Huế'),
        ('staff_hue', 'Lotte Cinema Huế'),
        ('staff_hue', 'BHD Star Huế'),
        ('staff_hue', 'Galaxy AEON Mall Huế'),
        ('staff_hue', 'Starlight Huế'),
        ('staff_blocked', 'Lotte Cinema Cần Thơ')
)
INSERT INTO staff_cinemas (staff_id, cinema_id, created_at)
SELECT u.id, c.id, NOW()
FROM assignments a
JOIN users u ON u.username = a.username
JOIN cinemas c ON c.name = a.cinema_name;

-- Mỗi rạp 3 phòng. Tổng: 69 phòng với dữ liệu hiện tại.
INSERT INTO rooms (id, cinema_id, name, created_at, updated_at, is_deleted)
SELECT
    uuid_generate_v4(),
    c.id,
    CASE
        WHEN room_idx = 1 THEN 'Phòng 01 - Standard'
        WHEN room_idx = 2 THEN 'Phòng 02 - Premium'
        ELSE 'Phòng 03 - IMAX'
    END,
    NOW(),
    NOW(),
    false
FROM cinemas c
CROSS JOIN generate_series(1, 3) AS room_idx;

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
(uuid_generate_v4(), 'WEEKDAY30K', 'Giảm 30.000đ cho suất chiếu ngày thường', 'FIXED', 30000, NULL, 150000, NOW() - INTERVAL '3 days', NOW() + INTERVAL '40 days', 400, 0, true, false, NOW(), NOW()),
(uuid_generate_v4(), 'FAMILY25', 'Giảm 25% cho đơn gia đình từ 4 vé', 'PERCENT', 25, 100000, 300000, NOW() - INTERVAL '5 days', NOW() + INTERVAL '50 days', 150, 12, true, false, NOW(), NOW()),
(uuid_generate_v4(), 'SEPAY15K', 'Giảm 15.000đ khi thanh toán bằng QR ngân hàng', 'FIXED', 15000, NULL, 100000, NOW() - INTERVAL '2 days', NOW() + INTERVAL '35 days', 600, 45, true, false, NOW(), NOW()),
(uuid_generate_v4(), 'VIP15', 'Giảm 15% cho khách hàng thân thiết', 'PERCENT', 15, 80000, 180000, NOW() - INTERVAL '10 days', NOW() + INTERVAL '70 days', 200, 25, true, false, NOW(), NOW()),
(uuid_generate_v4(), 'EXPIRED20', 'Mã đã hết hạn dùng để test trạng thái khuyến mãi', 'PERCENT', 20, 50000, 100000, NOW() - INTERVAL '70 days', NOW() - INTERVAL '5 days', 100, 18, false, false, NOW(), NOW()),
(uuid_generate_v4(), 'SOLDOUT10', 'Mã đã dùng hết lượt dùng để test giới hạn usage', 'PERCENT', 10, 40000, 100000, NOW() - INTERVAL '7 days', NOW() + INTERVAL '30 days', 30, 30, true, false, NOW(), NOW());

-- =========================================
-- 6. SHOWTIMES
-- =========================================
-- Sinh 5 khung giờ mỗi ngày trong 7 ngày tới cho tất cả phòng.
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
    SELECT generate_series(1, 7) AS day_offset
),
time_slots AS (
    SELECT * FROM (VALUES
        (1, TIME '09:00:00', 70000::numeric),
        (2, TIME '12:15:00', 80000::numeric),
        (3, TIME '15:30:00', 90000::numeric),
        (4, TIME '18:45:00', 110000::numeric),
        (5, TIME '21:45:00', 120000::numeric)
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
      AND r.name = 'Phòng 01 - Standard'
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
-- 8A. QUICK TEST DATA: STAFF HUẾ CHECK-IN
-- =========================================
-- Tạo một đơn SUCCESS tại BHD Star Huế để staff_hue test đúng scope rạp.
-- Suất bắt đầu sau 45 phút nên xuất hiện trong danh sách suất đang mở check-in.
WITH constants AS (
    SELECT
        '00000000-0000-0000-0000-000000000981'::uuid AS demo_showtime_id,
        '00000000-0000-0000-0000-000000000982'::uuid AS demo_booking_id,
        '00000000-0000-0000-0000-000000000983'::uuid AS demo_detail_1_id,
        '00000000-0000-0000-0000-000000000984'::uuid AS demo_detail_2_id,
        '00000000-0000-0000-0000-000000000985'::uuid AS demo_payment_id,
        '00000000-0000-0000-0000-000000000986'::uuid AS demo_ticket_1_id,
        '00000000-0000-0000-0000-000000000987'::uuid AS demo_ticket_2_id,
        'BBBBBBBBBBBBBBBBBBBBBB'::text AS demo_nonce,
        'eM4ritLt9dfucTMfTTTX5afCVKnWD1OH6YTOrO6RnMyDIBkLk4al8oZUKTBnIKvwthu5TbEROewKIoYsktEAa3'::text AS demo_qr_secret
),
demo_movie AS (
    SELECT id, duration
    FROM movies
    WHERE title = 'Dune: Part Two'
    LIMIT 1
),
demo_room AS (
    SELECT r.id
    FROM rooms r
    JOIN cinemas c ON c.id = r.cinema_id
    WHERE c.name = 'BHD Star Huế'
      AND r.name = 'Phòng 02 - Premium'
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
        NOW() + INTERVAL '45 minutes',
        NOW() + INTERVAL '3 hours 31 minutes',
        85000,
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
            WHEN s.row_label = 'G' AND s.seat_number IN (5, 6) THEN 'BOOKED'
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
    WHERE username = 'user2'
    LIMIT 1
),
selected_demo_seats AS (
    SELECT
        s.id,
        st.base_price * s.price_multiplier AS price_at_booking,
        row_number() OVER (ORDER BY s.seat_number) AS seat_no
    FROM inserted_showtime st
    JOIN seats s ON s.room_id = st.room_id
    WHERE s.row_label = 'G'
      AND s.seat_number IN (5, 6)
),
inserted_booking AS (
    INSERT INTO bookings (
        id, user_id, showtime_id, promotion_id, total_price,
        discount_amount, status, secure_token, payment_expires_at, created_at, updated_at
    )
    SELECT
        c.demo_booking_id,
        u.id,
        st.id,
        NULL,
        (SELECT sum(s.price_at_booking) FROM selected_demo_seats s),
        0,
        'SUCCESS',
        'demo-hue-success-booking-token',
        NOW() + INTERVAL '10 minutes',
        NOW(),
        NOW()
    FROM constants c
    CROSS JOIN demo_user u
    CROSS JOIN inserted_showtime st
    RETURNING id, total_price
),
inserted_booking_details AS (
    INSERT INTO booking_details (
        id, booking_id, seat_id, price_at_booking, created_at, updated_at
    )
    SELECT
        CASE WHEN s.seat_no = 1 THEN c.demo_detail_1_id ELSE c.demo_detail_2_id END,
        b.id,
        s.id,
        s.price_at_booking,
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
        b.total_price,
        'SEPAY',
        'DEMO_SEPAY_HUE_SUCCESS_001',
        'SUCCESS',
        jsonb_build_object('demo', true, 'message', 'Seeded SePay payment for Hue staff check-in test'),
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
SELECT 'Hue staff quick test ticket QR: ' || qr_code AS demo_hue_ticket_qr
FROM inserted_tickets;

-- =========================================
-- 8B. QUICK TEST DATA: ORDER STATUS CASES
-- =========================================
-- Các case này dùng để test tab "Đơn đã đặt" trên client:
-- 1) PENDING_FUTURE: còn hạn thanh toán -> hiện nút Thanh toán.
-- 2) FAILED_FUTURE: thanh toán thất bại, suất còn tương lai -> hiện nút Chọn lại ghế.
-- 3) EXPIRED_PAST: đơn hết hạn, suất đã qua -> hiện nhãn Suất chiếu đã qua.
-- 4) CANCELLED_PAST: đơn đã hủy, suất đã qua -> hiện nhãn Suất chiếu đã qua.
-- 5) REFUND_PENDING: suất bị rạp hủy sau khi thanh toán -> hiện Đang xử lý hoàn tiền.
WITH demo_user AS (
    SELECT id
    FROM users
    WHERE username = 'user1'
    LIMIT 1
),
demo_room AS (
    SELECT r.id
    FROM rooms r
    JOIN cinemas c ON c.id = r.cinema_id
    WHERE c.name = 'CGV Sư Vạn Hạnh'
      AND r.name = 'Phòng 01 - Standard'
    LIMIT 1
),
demo_movies AS (
    SELECT title, id
    FROM movies
    WHERE title IN (
        'Dune: Part Two',
        'Inside Out 2',
        'Twisters',
        'Mai',
        'Furiosa: Câu Chuyện Từ Max Điên'
    )
),
cases AS (
    SELECT *
    FROM (VALUES
        (
            'PENDING_FUTURE',
            '00000000-0000-0000-0000-000000000911'::uuid,
            '00000000-0000-0000-0000-000000000912'::uuid,
            '00000000-0000-0000-0000-000000000913'::uuid,
            'Dune: Part Two',
            'B',
            ARRAY[1, 2]::int[],
            NOW() + INTERVAL '2 hours',
            NOW() + INTERVAL '4 hours',
            'UPCOMING',
            'PENDING',
            'PENDING',
            'demo-pending-booking-token',
            NOW() + INTERVAL '5 minutes',
            90000::numeric
        ),
        (
            'FAILED_FUTURE',
            '00000000-0000-0000-0000-000000000921'::uuid,
            '00000000-0000-0000-0000-000000000922'::uuid,
            '00000000-0000-0000-0000-000000000923'::uuid,
            'Inside Out 2',
            'C',
            ARRAY[1, 2]::int[],
            NOW() + INTERVAL '3 hours',
            NOW() + INTERVAL '5 hours',
            'UPCOMING',
            'FAILED',
            'FAILED',
            'demo-failed-booking-token',
            NOW() - INTERVAL '5 minutes',
            80000::numeric
        ),
        (
            'EXPIRED_PAST',
            '00000000-0000-0000-0000-000000000931'::uuid,
            '00000000-0000-0000-0000-000000000932'::uuid,
            '00000000-0000-0000-0000-000000000933'::uuid,
            'Twisters',
            'D',
            ARRAY[1, 2]::int[],
            NOW() - INTERVAL '4 hours',
            NOW() - INTERVAL '2 hours',
            'ENDED',
            'EXPIRED',
            'EXPIRED',
            'demo-expired-booking-token',
            NOW() - INTERVAL '3 hours',
            85000::numeric
        ),
        (
            'CANCELLED_PAST',
            '00000000-0000-0000-0000-000000000941'::uuid,
            '00000000-0000-0000-0000-000000000942'::uuid,
            NULL::uuid,
            'Mai',
            'E',
            ARRAY[1, 2]::int[],
            NOW() - INTERVAL '5 hours',
            NOW() - INTERVAL '3 hours',
            'ENDED',
            'CANCELLED',
            NULL,
            'demo-cancelled-booking-token',
            NOW() - INTERVAL '4 hours',
            75000::numeric
        ),
        (
            'REFUND_PENDING',
            '00000000-0000-0000-0000-000000000951'::uuid,
            '00000000-0000-0000-0000-000000000952'::uuid,
            '00000000-0000-0000-0000-000000000953'::uuid,
            'Furiosa: Câu Chuyện Từ Max Điên',
            'F',
            ARRAY[1, 2]::int[],
            NOW() + INTERVAL '6 hours',
            NOW() + INTERVAL '8 hours',
            'CANCELLED',
            'REFUND_PENDING',
            'REFUND_PENDING',
            'demo-refund-pending-booking-token',
            NOW() - INTERVAL '1 hour',
            120000::numeric
        )
    ) AS t(
        case_key, showtime_id, booking_id, payment_id, movie_title,
        row_label, seat_numbers, start_time, end_time, showtime_status,
        booking_status, payment_status, secure_token, payment_expires_at, base_price
    )
),
inserted_showtimes AS (
    INSERT INTO showtimes (
        id, movie_id, room_id, start_time, end_time,
        base_price, status, created_at, updated_at, is_deleted
    )
    SELECT
        c.showtime_id,
        m.id,
        r.id,
        c.start_time,
        c.end_time,
        c.base_price,
        c.showtime_status,
        NOW(),
        NOW(),
        false
    FROM cases c
    JOIN demo_movies m ON m.title = c.movie_title
    CROSS JOIN demo_room r
    RETURNING id, room_id
),
inserted_case_seat_status AS (
    INSERT INTO seat_status (
        id, showtime_id, seat_id, status, hold_by, hold_until, version, created_at, updated_at
    )
    SELECT
        uuid_generate_v4(),
        st.id,
        s.id,
        CASE
            WHEN c.booking_status = 'PENDING'
             AND s.row_label = c.row_label
             AND s.seat_number = ANY(c.seat_numbers)
                THEN 'HOLD'
            ELSE 'AVAILABLE'
        END,
        CASE
            WHEN c.booking_status = 'PENDING'
             AND s.row_label = c.row_label
             AND s.seat_number = ANY(c.seat_numbers)
                THEN u.id
            ELSE NULL
        END,
        CASE
            WHEN c.booking_status = 'PENDING'
             AND s.row_label = c.row_label
             AND s.seat_number = ANY(c.seat_numbers)
                THEN c.payment_expires_at
            ELSE NULL
        END,
        0,
        NOW(),
        NOW()
    FROM inserted_showtimes st
    JOIN cases c ON c.showtime_id = st.id
    JOIN seats s ON s.room_id = st.room_id
    CROSS JOIN demo_user u
    WHERE s.is_deleted = false
    RETURNING seat_id
),
inserted_bookings AS (
    INSERT INTO bookings (
        id, user_id, showtime_id, promotion_id, total_price,
        discount_amount, status, secure_token, payment_expires_at, created_at, updated_at
    )
    SELECT
        c.booking_id,
        u.id,
        c.showtime_id,
        NULL,
        c.base_price * 2,
        0,
        c.booking_status,
        c.secure_token,
        c.payment_expires_at,
        NOW(),
        NOW()
    FROM cases c
    CROSS JOIN demo_user u
    RETURNING id
),
selected_case_seats AS (
    SELECT
        c.case_key,
        c.booking_id,
        c.base_price,
        s.id AS seat_id
    FROM cases c
    JOIN inserted_showtimes st ON st.id = c.showtime_id
    JOIN seats s ON s.room_id = st.room_id
    WHERE s.row_label = c.row_label
      AND s.seat_number = ANY(c.seat_numbers)
),
inserted_case_booking_details AS (
    INSERT INTO booking_details (
        id, booking_id, seat_id, price_at_booking, created_at, updated_at
    )
    SELECT
        uuid_generate_v4(),
        s.booking_id,
        s.seat_id,
        s.base_price,
        NOW(),
        NOW()
    FROM selected_case_seats s
    RETURNING id
),
inserted_case_payments AS (
    INSERT INTO payments (
        id, booking_id, amount, method, transaction_no,
        status, provider_response, payment_time, created_at, updated_at
    )
    SELECT
        c.payment_id,
        c.booking_id,
        c.base_price * 2,
        'VNPAY',
        'DEMO_' || c.case_key,
        c.payment_status,
        jsonb_build_object('demo', true, 'case', c.case_key),
        CASE WHEN c.payment_status = 'PENDING' THEN NULL ELSE NOW() END,
        NOW(),
        NOW()
    FROM cases c
    WHERE c.payment_id IS NOT NULL
      AND c.payment_status IS NOT NULL
    RETURNING id, booking_id, amount, method
),
inserted_case_refunds AS (
    INSERT INTO refunds (
        id, booking_id, payment_id, amount, method, status, reason,
        requested_at, created_at, updated_at
    )
    SELECT
        uuid_generate_v4(),
        p.booking_id,
        p.id,
        p.amount,
        p.method,
        'PENDING',
        'Suất chiếu bị rạp hủy, hệ thống đang xử lý hoàn tiền.',
        NOW(),
        NOW(),
        NOW()
    FROM inserted_case_payments p
    JOIN cases c ON c.booking_id = p.booking_id
    WHERE c.case_key = 'REFUND_PENDING'
    RETURNING id
)
SELECT
    'Quick test order cases seeded: PENDING_FUTURE, FAILED_FUTURE, EXPIRED_PAST, CANCELLED_PAST, REFUND_PENDING' AS demo_order_cases,
    (SELECT count(*) FROM inserted_case_refunds) AS demo_refund_requests;

-- =========================================
-- 8C. DEMO SALES DATA: BOOKINGS + PAYMENTS + TICKETS + PAYMENT EVENTS
-- =========================================
-- Sinh 60 đơn SUCCESS để dashboard, quản lý thanh toán, đơn đặt vé, vé của tôi và top phim có dữ liệu đẹp.
WITH constants AS (
    SELECT
        'SALEDEMOAAAAAAAAAAAA'::text AS sale_nonce,
        'eM4ritLt9dfucTMfTTTX5afCVKnWD1OH6YTOrO6RnMyDIBkLk4al8oZUKTBnIKvwthu5TbEROewKIoYsktEAa3'::text AS demo_qr_secret
),
demo_users AS (
    SELECT
        id,
        username,
        row_number() OVER (ORDER BY username) AS user_no,
        count(*) OVER () AS user_count
    FROM users
    WHERE username IN ('user1', 'user2', 'user3', 'user4', 'user5', 'user6', 'user7', 'user8', 'user_vip')
      AND is_deleted = false
      AND is_active = true
),
demo_staff AS (
    SELECT id
    FROM users
    WHERE username = 'staff1'
    LIMIT 1
),
ranked_showtimes AS (
    SELECT
        st.id AS showtime_id,
        st.room_id,
        st.base_price,
        row_number() OVER (ORDER BY st.start_time, c.name, r.name, m.title) AS sale_no
    FROM showtimes st
    JOIN movies m ON m.id = st.movie_id
    JOIN rooms r ON r.id = st.room_id
    JOIN cinemas c ON c.id = r.cinema_id
    WHERE st.is_deleted = false
      AND st.status = 'UPCOMING'
      AND st.start_time > NOW() + INTERVAL '1 day'
      AND st.id <> '00000000-0000-0000-0000-000000000901'::uuid
),
sale_orders AS (
    SELECT
        rs.sale_no,
        rs.showtime_id,
        rs.room_id,
        rs.base_price,
        du.id AS user_id,
        CASE
            WHEN rs.sale_no % 4 = 0 THEN 'F'
            WHEN rs.sale_no % 4 = 1 THEN 'C'
            WHEN rs.sale_no % 4 = 2 THEN 'D'
            ELSE 'E'
        END AS row_label,
        ARRAY[((rs.sale_no - 1) % 10) + 1, ((rs.sale_no - 1) % 10) + 2]::int[] AS seat_numbers,
        CASE
            WHEN rs.sale_no % 5 = 0 THEN 30000::numeric
            WHEN rs.sale_no % 7 = 0 THEN 15000::numeric
            ELSE 0::numeric
        END AS discount_amount,
        CASE
            WHEN rs.sale_no % 3 = 0 THEN 'SEPAY'
            WHEN rs.sale_no % 3 = 1 THEN 'VNPAY'
            ELSE 'CASH'
        END AS payment_method,
        NOW() - ((rs.sale_no % 28) || ' days')::interval - ((rs.sale_no % 8) || ' hours')::interval AS created_at
    FROM ranked_showtimes rs
    JOIN demo_users du
      ON du.user_no = (((rs.sale_no - 1) % du.user_count) + 1)
    WHERE rs.sale_no <= 60
),
inserted_sale_bookings AS (
    INSERT INTO bookings (
        id, user_id, showtime_id, promotion_id, total_price,
        discount_amount, status, secure_token, payment_expires_at, created_at, updated_at
    )
    SELECT
        uuid_generate_v4(),
        so.user_id,
        so.showtime_id,
        NULL,
        GREATEST((so.base_price * 2) - so.discount_amount, 0),
        so.discount_amount,
        'SUCCESS',
        'demo-sale-booking-' || lpad(so.sale_no::text, 3, '0'),
        so.created_at + INTERVAL '10 minutes',
        so.created_at,
        so.created_at
    FROM sale_orders so
    RETURNING id, showtime_id, total_price, created_at, secure_token
),
selected_sale_seats AS (
    SELECT
        so.sale_no,
        b.id AS booking_id,
        so.showtime_id,
        so.base_price,
        s.id AS seat_id
    FROM sale_orders so
    JOIN inserted_sale_bookings b ON b.showtime_id = so.showtime_id
    JOIN seats s ON s.room_id = so.room_id
    WHERE s.row_label = so.row_label
      AND s.seat_number = ANY(so.seat_numbers)
),
inserted_sale_booking_details AS (
    INSERT INTO booking_details (
        id, booking_id, seat_id, price_at_booking, created_at, updated_at
    )
    SELECT
        uuid_generate_v4(),
        s.booking_id,
        s.seat_id,
        s.base_price,
        b.created_at,
        b.created_at
    FROM selected_sale_seats s
    JOIN inserted_sale_bookings b ON b.id = s.booking_id
    RETURNING id, booking_id
),
updated_sale_seat_status AS (
    UPDATE seat_status ss
    SET status = 'BOOKED',
        hold_by = NULL,
        hold_until = NULL,
        updated_at = NOW()
    FROM selected_sale_seats s
    WHERE ss.showtime_id = s.showtime_id
      AND ss.seat_id = s.seat_id
    RETURNING ss.id
),
inserted_sale_payments AS (
    INSERT INTO payments (
        id, booking_id, amount, method, transaction_no,
        status, provider_response, payment_time, created_at, updated_at
    )
    SELECT
        uuid_generate_v4(),
        b.id,
        b.total_price,
        so.payment_method,
        'DEMO_SALE_' || lpad(so.sale_no::text, 3, '0'),
        'SUCCESS',
        jsonb_build_object(
            'demo', true,
            'source', 'mock-data',
            'method', so.payment_method,
            'secureToken', b.secure_token
        ),
        b.created_at + INTERVAL '2 minutes',
        b.created_at + INTERVAL '2 minutes',
        b.created_at + INTERVAL '2 minutes'
    FROM inserted_sale_bookings b
    JOIN sale_orders so ON so.showtime_id = b.showtime_id
    RETURNING id, booking_id, method, transaction_no, status, created_at
),
ticket_payloads AS (
    SELECT
        bd.id AS booking_detail_id,
        b.id AS booking_id,
        so.sale_no,
        'CBT1.' || replace(upper(bd.id::text), '-', '') || '.' || c.sale_nonce AS payload,
        c.demo_qr_secret,
        b.created_at
    FROM inserted_sale_booking_details bd
    JOIN inserted_sale_bookings b ON b.id = bd.booking_id
    JOIN sale_orders so ON so.showtime_id = b.showtime_id
    CROSS JOIN constants c
),
inserted_sale_tickets AS (
    INSERT INTO tickets (
        id, booking_detail_id, qr_code, status, check_in_time, checked_in_by, created_at, updated_at
    )
    SELECT
        uuid_generate_v4(),
        p.booking_detail_id,
        p.payload || '.' || translate(
            rtrim(encode(substring(hmac(p.payload, p.demo_qr_secret, 'sha256') from 1 for 24), 'base64'), '='),
            '+/',
            '-_'
        ),
        CASE WHEN p.sale_no % 6 = 0 THEN 'USED' ELSE 'ACTIVE' END,
        CASE WHEN p.sale_no % 6 = 0 THEN NOW() - ((p.sale_no % 6) || ' hours')::interval ELSE NULL END,
        CASE WHEN p.sale_no % 6 = 0 THEN ds.id ELSE NULL END,
        p.created_at,
        p.created_at
    FROM ticket_payloads p
    CROSS JOIN demo_staff ds
    RETURNING id
),
inserted_sale_payment_events AS (
    INSERT INTO payment_events (
        id, payment_id, booking_id, method, transaction_no, event_type,
        payment_status_before, payment_status_after, booking_status_before, booking_status_after,
        success, message, payload, created_at, updated_at
    )
    SELECT
        uuid_generate_v4(),
        p.id,
        p.booking_id,
        p.method,
        p.transaction_no,
        'PAYMENT_SUCCESS',
        'PENDING',
        p.status,
        'PENDING',
        'SUCCESS',
        true,
        'Demo payment confirmed for sales analytics',
        jsonb_build_object('demo', true, 'transactionNo', p.transaction_no),
        p.created_at,
        p.created_at
    FROM inserted_sale_payments p
    RETURNING id
)
SELECT 'Demo sales seeded: 60 success bookings, 120 tickets, payment events and booked seats' AS demo_sales_data;

-- =========================================
-- 8D. DEMO AUDIT DATA
-- =========================================
INSERT INTO auth_audit_logs (
    id, user_id, username, event_type, success, failure_reason,
    ip_address, user_agent, created_at, updated_at
)
SELECT
    uuid_generate_v4(),
    u.id,
    u.username,
    seed.event_type,
    seed.success,
    seed.failure_reason,
    seed.ip_address,
    seed.user_agent,
    seed.created_at,
    seed.created_at
FROM (
    VALUES
        ('user1', 'LOGIN_SUCCESS', true, NULL, '127.0.0.1', 'Chrome Dev', NOW() - INTERVAL '2 hours'),
        ('user2', 'LOGIN_SUCCESS', true, NULL, '127.0.0.1', 'Chrome Dev', NOW() - INTERVAL '3 hours'),
        ('staff1', 'LOGIN_SUCCESS', true, NULL, '127.0.0.1', 'Chrome Dev', NOW() - INTERVAL '4 hours'),
        ('staff_hcm', 'LOGIN_SUCCESS', true, NULL, '127.0.0.1', 'Chrome Dev', NOW() - INTERVAL '5 hours'),
        ('user_blocked', 'LOGIN_FAILED', false, 'Account is blocked', '127.0.0.1', 'Chrome Dev', NOW() - INTERVAL '6 hours'),
        ('user_pending', 'LOGIN_FAILED', false, 'Email is not verified', '127.0.0.1', 'Chrome Dev', NOW() - INTERVAL '7 hours'),
        ('user3', 'PASSWORD_RESET_REQUESTED', true, NULL, '127.0.0.1', 'Chrome Dev', NOW() - INTERVAL '1 day'),
        ('user_vip', 'REFRESH_TOKEN_SUCCESS', true, NULL, '127.0.0.1', 'Chrome Dev', NOW() - INTERVAL '30 minutes')
) AS seed(username, event_type, success, failure_reason, ip_address, user_agent, created_at)
JOIN users u ON u.username = seed.username;

INSERT INTO admin_audit_logs (
    id, actor_id, actor_username, http_method, action, resource, resource_id,
    request_path, query_string, ip_address, user_agent, status_code, success,
    error_message, created_at, updated_at
)
SELECT
    uuid_generate_v4(),
    admin_user.id,
    COALESCE(admin_user.username, 'admin'),
    seed.http_method,
    seed.action,
    seed.resource,
    seed.resource_id,
    seed.request_path,
    seed.query_string,
    '127.0.0.1',
    'Chrome Dev',
    seed.status_code,
    seed.success,
    seed.error_message,
    seed.created_at,
    seed.created_at
FROM (
    VALUES
        ('POST', 'CREATE', 'showtimes', NULL, '/api/v1/showtimes', NULL, 201, true, NULL, NOW() - INTERVAL '5 hours'),
        ('PUT', 'UPDATE', 'users', 'staff_hcm', '/api/v1/users/staff_hcm', NULL, 200, true, NULL, NOW() - INTERVAL '4 hours'),
        ('POST', 'ASSIGN_STAFF_CINEMA', 'users', 'staff_hanoi', '/api/v1/users/staff_hanoi', NULL, 200, true, NULL, NOW() - INTERVAL '3 hours'),
        ('PATCH', 'BLOCK', 'users', 'user_blocked', '/api/v1/users/user_blocked/block', NULL, 200, true, NULL, NOW() - INTERVAL '2 hours'),
        ('POST', 'CANCEL_SHOWTIME', 'showtimes', 'demo-cancelled-showtime', '/api/v1/showtimes/demo-cancelled-showtime/cancel', NULL, 409, false, 'Only upcoming or ongoing showtimes can be cancelled', NOW() - INTERVAL '1 hour')
) AS seed(http_method, action, resource, resource_id, request_path, query_string, status_code, success, error_message, created_at)
LEFT JOIN LATERAL (
    SELECT id, username
    FROM users
    WHERE username = 'admin'
    LIMIT 1
) admin_user ON true;

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

    IF (
        SELECT count(*)
        FROM users
        WHERE username IN (
            'staff1', 'staff_hcm', 'staff_hanoi', 'staff_danang', 'staff_hue', 'staff_unassigned', 'staff_blocked',
            'user1', 'user2', 'user3', 'user4', 'user5', 'user6', 'user7', 'user8', 'user_vip', 'user_pending', 'user_blocked'
        )
    ) < 18 THEN
        RAISE EXCEPTION 'Mock data invalid: missing test users/staff accounts.';
    END IF;

    IF (SELECT count(*) FROM staff_cinemas) < 23 THEN
        RAISE EXCEPTION 'Mock data invalid: staff cinema assignments were not seeded correctly.';
    END IF;

    IF (
        SELECT count(*)
        FROM cinemas
        WHERE city = 'Huế'
          AND is_active = true
          AND is_deleted = false
    ) < 5 THEN
        RAISE EXCEPTION 'Mock data invalid: Hue cinema cluster was not seeded correctly.';
    END IF;

    IF (
        SELECT count(*)
        FROM staff_cinemas sc
        JOIN users u ON u.id = sc.staff_id
        JOIN cinemas c ON c.id = sc.cinema_id
        WHERE u.username = 'staff_hue'
          AND c.city = 'Huế'
          AND c.is_deleted = false
    ) < 5 THEN
        RAISE EXCEPTION 'Mock data invalid: staff_hue cinema scope was not seeded correctly.';
    END IF;

    IF (
        SELECT count(*)
        FROM cinemas c
        WHERE c.city = 'Quảng Nam'
          AND c.is_deleted = false
          AND (
              6371 * 2 * atan2(
                  sqrt(
                      power(sin(radians(c.latitude - 15.593163) / 2), 2) +
                      cos(radians(15.593163)) * cos(radians(c.latitude)) *
                      power(sin(radians(c.longitude - 108.534505) / 2), 2)
                  ),
                  sqrt(1 - (
                      power(sin(radians(c.latitude - 15.593163) / 2), 2) +
                      cos(radians(15.593163)) * cos(radians(c.latitude)) *
                      power(sin(radians(c.longitude - 108.534505) / 2), 2)
                  ))
              )
          ) <= 10
    ) < 5 THEN
        RAISE EXCEPTION 'Mock data invalid: nearby Quang Nam cinemas for geolocation testing were not seeded correctly.';
    END IF;

    IF (
        SELECT count(*)
        FROM bookings
        WHERE secure_token LIKE 'demo-sale-booking-%'
    ) <> 60 THEN
        RAISE EXCEPTION 'Mock data invalid: demo sales bookings were not seeded correctly.';
    END IF;

    IF (
        SELECT count(*)
        FROM tickets t
        JOIN booking_details bd ON bd.id = t.booking_detail_id
        JOIN bookings b ON b.id = bd.booking_id
        WHERE b.secure_token LIKE 'demo-sale-booking-%'
    ) <> 120 THEN
        RAISE EXCEPTION 'Mock data invalid: demo sales tickets were not seeded correctly.';
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM refunds r
        JOIN bookings b ON b.id = r.booking_id
        JOIN payments p ON p.id = r.payment_id
        WHERE b.secure_token = 'demo-refund-pending-booking-token'
          AND b.status = 'REFUND_PENDING'
          AND p.status = 'REFUND_PENDING'
          AND r.status = 'PENDING'
    ) THEN
        RAISE EXCEPTION 'Mock data invalid: refund pending case was not seeded correctly.';
    END IF;

    IF (
        SELECT count(*)
        FROM tickets t
        JOIN booking_details bd ON bd.id = t.booking_detail_id
        JOIN bookings b ON b.id = bd.booking_id
        JOIN showtimes st ON st.id = b.showtime_id
        JOIN rooms room ON room.id = st.room_id
        JOIN cinemas c ON c.id = room.cinema_id
        WHERE b.secure_token = 'demo-hue-success-booking-token'
          AND b.status = 'SUCCESS'
          AND t.status = 'ACTIVE'
          AND c.name = 'BHD Star Huế'
    ) <> 2 THEN
        RAISE EXCEPTION 'Mock data invalid: Hue staff check-in tickets were not seeded correctly.';
    END IF;
END $$;

-- Tài khoản test:
-- admin: chỉ được tạo khi bật APP_BOOTSTRAP_ADMIN_ENABLED và khai báo APP_BOOTSTRAP_ADMIN_USERNAME/PASSWORD.
-- Tất cả tài khoản mock bên dưới có mật khẩu 123456.
-- STAFF:
-- staff1: phụ trách CGV Sư Vạn Hạnh, BHD Star Bitexco.
-- staff_hcm: phụ trách cụm TP Hồ Chí Minh.
-- staff_hanoi: phụ trách cụm Hà Nội.
-- staff_danang: phụ trách cụm Đà Nẵng và Quảng Nam để test scope rạp gần bạn.
-- staff_hue: phụ trách 5 rạp tại Huế, dùng để test scope, lịch chiếu và soát vé theo rạp.
-- staff_unassigned: chưa gán rạp, dùng để test filter "Chưa gán rạp".
-- staff_blocked: tài khoản staff bị khóa, dùng để test block/unblock.
-- USER:
-- user1, user2, user3, user4, user5, user6, user7, user8, user_vip.
-- user_pending: email_verified=false, dùng để test xác thực email.
-- user_blocked: tài khoản user bị khóa, dùng để test đăng nhập/block.
-- Kỳ vọng dữ liệu:
-- 23 phim NOW_SHOWING, 2 phim không chiếu hiện tại, 23 rạp, 69 phòng, 6624 ghế.
-- 2422 suất chiếu, 232512 dòng seat_status, 67 booking, 66 payment, 124 ticket, 1 refund pending.
-- Test nút "Gần tôi" trên tab Rạp chiếu:
-- Dùng vị trí 15.593163, 108.534505 sẽ thấy cụm rạp Quảng Nam được sắp xếp gần nhất.
-- Trong 10km có CinemaBooking Tam Kỳ Center, Galaxy Tam Kỳ Square, Beta Tam Phú, Cinestar Tam Thăng, Mega GS An Hà.
-- Lotte Cinema Chu Lai nằm xa hơn để test badge khoảng cách ngoài bán kính gần.
-- Vé test nhanh:
-- user1 có booking SUCCESS tại CGV Sư Vạn Hạnh, Phòng 01 - Standard, ghế A1/A2, suất chiếu bắt đầu sau 30 phút.
-- Lấy QR để staff check-in:
-- SELECT t.qr_code
-- FROM tickets t
-- JOIN booking_details bd ON bd.id = t.booking_detail_id
-- JOIN bookings b ON b.id = bd.booking_id
-- WHERE b.secure_token = 'demo-success-booking-token';
-- Vé test scope staff Huế:
-- user2 có booking SUCCESS tại BHD Star Huế, Phòng 02 - Premium, ghế G5/G6, suất bắt đầu sau 45 phút.
-- Đăng nhập staff_hue / 123456, chọn BHD Star Huế và suất đang mở check-in rồi quét QR lấy bằng:
-- SELECT t.qr_code
-- FROM tickets t
-- JOIN booking_details bd ON bd.id = t.booking_detail_id
-- JOIN bookings b ON b.id = bd.booking_id
-- WHERE b.secure_token = 'demo-hue-success-booking-token';
-- Có thể dùng QR Huế với staff1 hoặc chọn sai rạp/sai suất để test các nhánh từ chối mà vé vẫn ACTIVE.
-- Case test tab "Đơn đã đặt" cho user1:
-- SUCCESS: demo-success-booking-token, suất sau 30 phút, ghế A1/A2, có QR.
-- PENDING: demo-pending-booking-token, suất sau 2 giờ, ghế B1/B2, còn hạn thanh toán.
-- FAILED: demo-failed-booking-token, suất sau 3 giờ, ghế C1/C2, có thể chọn lại ghế.
-- EXPIRED: demo-expired-booking-token, suất đã qua, không hiện nút chọn lại ghế.
-- CANCELLED: demo-cancelled-booking-token, suất đã qua, không hiện nút chọn lại ghế.
-- REFUND_PENDING: demo-refund-pending-booking-token, suất bị rạp hủy, hiện Đang xử lý hoàn tiền.
-- Dữ liệu doanh thu:
-- 60 booking SUCCESS tự sinh với secure_token demo-sale-booking-001 đến demo-sale-booking-060.
-- Payment method được xoay vòng VNPAY / SEPAY / CASH để test bộ lọc thanh toán.
-- Một phần ticket đã USED và có checked_in_by=staff1 để test lịch sử soát vé.
