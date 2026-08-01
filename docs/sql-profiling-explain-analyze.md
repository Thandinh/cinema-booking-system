# SQL Profiling, EXPLAIN ANALYZE và Cache

Tài liệu này dùng để kiểm tra hiệu năng query trong CinemaBookingSystem mà không làm nhiễu môi trường chạy bình thường.

## 1. Khi nào cần bật SQL profiling?

Chỉ bật khi đang debug hoặc chuẩn bị bảo vệ:

- Muốn biết một API đang chạy bao nhiêu SQL.
- Muốn kiểm tra có N+1 query không.
- Muốn đo query nào chậm.
- Muốn chứng minh hệ thống đã có index và query hợp lý.

Không nên bật profiling trong production thật vì log SQL và bind parameter có thể rất nhiều, làm giảm hiệu năng và dễ lộ dữ liệu nhạy cảm.

## 2. Cách bật SQL profiling

Chạy backend với profile riêng:

```powershell
mvn spring-boot:run -Dspring-boot.run.profiles=sql-profile
```

Hoặc dùng biến môi trường:

```powershell
$env:SPRING_PROFILES_ACTIVE="sql-profile"
$env:HIBERNATE_SLOW_QUERY_MS="50"
mvn spring-boot:run
```

Các biến quan trọng:

- `HIBERNATE_SQL_LOG_LEVEL=DEBUG`: log câu SQL.
- `HIBERNATE_BIND_LOG_LEVEL=TRACE`: log tham số truyền vào SQL.
- `HIBERNATE_GENERATE_STATISTICS=true`: bật thống kê Hibernate.
- `HIBERNATE_SLOW_QUERY_MS=50`: log query chậm hơn 50ms.
- `CACHE_CAFFEINE_SPEC=maximumSize=1000,expireAfterWrite=10m`: chỉnh TTL/size cache.

## 3. Cache đang dùng cho dữ liệu nào?

Cache dùng Caffeine local cache, phù hợp monolith/dev/small production.

Đang cache:

- `movies`: danh sách phim, trừ sort `POPULAR` vì phụ thuộc số vé bán.
- `cinemas`: danh sách/chi tiết rạp.
- `cinema-map`: dữ liệu marker trên bản đồ.
- `rooms-by-cinema`: danh sách phòng theo rạp.
- `seats-by-room`: sơ đồ ghế cấu hình theo phòng.
- `promotions`: danh sách mã khuyến mãi đang khả dụng.

Không cache:

- `seat_status`: realtime giữ ghế/đặt ghế.
- `bookings`: đơn đặt vé.
- `payments`: thanh toán.
- `tickets`: vé/QR/check-in.
- Dashboard realtime hoặc báo cáo doanh thu nóng.

Lý do: các dữ liệu này thay đổi liên tục, nếu cache sai sẽ gây lệch trạng thái ghế, vé hoặc tiền.

## 4. Quy tắc đọc log để phát hiện N+1

Ví dụ gọi API lấy danh sách booking 20 dòng:

- Tốt: 1 query lấy ID page, 1 query fetch detail theo ID, 1 query count.
- Xấu: 1 query danh sách + 20 query lấy user/movie/seat/ticket từng dòng.

Dấu hiệu N+1:

- Log lặp lại cùng một câu SQL nhiều lần, chỉ khác `id = ?`.
- Số query tăng theo số item trả về.
- Khi tăng page size từ 10 lên 50, số query tăng rất mạnh.

## 5. EXPLAIN ANALYZE cho các query nóng

Chạy trong PostgreSQL console hoặc pgAdmin. Thay UUID/ngày bằng dữ liệu thật trong database.

### 5.1. Seat map theo suất chiếu

```sql
EXPLAIN (ANALYZE, BUFFERS)
SELECT ss.*
FROM seat_status ss
JOIN seats s ON s.id = ss.seat_id
WHERE ss.showtime_id = '00000000-0000-0000-0000-000000000901'
ORDER BY s.row_index, s.col_index;
```

Kỳ vọng:

- Dùng index trên `seat_status(showtime_id, status)` hoặc unique `(seat_id, showtime_id)`.
- Số dòng trả về xấp xỉ số ghế của phòng.

### 5.2. Dọn hold hết hạn

```sql
EXPLAIN (ANALYZE, BUFFERS)
SELECT ss.id
FROM seat_status ss
WHERE ss.status = 'HOLD'
  AND ss.hold_until <= NOW()
ORDER BY ss.hold_until
LIMIT 500;
```

Kỳ vọng:

- Dùng index liên quan `hold_until`.
- Không scan toàn bộ bảng lớn khi hệ thống nhiều suất chiếu.

### 5.3. Dọn booking PENDING hết hạn

```sql
EXPLAIN (ANALYZE, BUFFERS)
SELECT b.id
FROM bookings b
WHERE b.status = 'PENDING'
  AND b.payment_expires_at <= NOW()
ORDER BY b.payment_expires_at
LIMIT 200;
```

Kỳ vọng:

- Có index theo `status/payment_expires_at`.
- Query chạy ổn định khi bảng bookings lớn.

### 5.4. Tìm payment theo mã giao dịch

```sql
EXPLAIN (ANALYZE, BUFFERS)
SELECT p.*
FROM payments p
WHERE p.transaction_no = '20260723142612827568472';
```

Kỳ vọng:

- Nên dùng index nếu lookup transaction nhiều.
- Với webhook/payment callback, query này cần nhanh và chính xác.

### 5.5. Dashboard doanh thu theo ngày

```sql
EXPLAIN (ANALYZE, BUFFERS)
SELECT DATE(p.payment_time) AS revenue_date,
       SUM(p.amount) AS revenue
FROM payments p
WHERE p.status = 'SUCCESS'
  AND p.payment_time >= CURRENT_DATE - INTERVAL '30 days'
GROUP BY DATE(p.payment_time)
ORDER BY revenue_date;
```

Kỳ vọng:

- Dùng index theo `status/payment_time`.
- Nếu dữ liệu rất lớn, sau này có thể tách read model/materialized view.

### 5.6. Phim bán chạy

```sql
EXPLAIN (ANALYZE, BUFFERS)
SELECT m.id, m.title, COUNT(b.id) AS booking_count, COALESCE(SUM(b.total_price), 0) AS revenue
FROM movies m
JOIN showtimes st ON st.movie_id = m.id
JOIN bookings b ON b.showtime_id = st.id
WHERE b.status = 'SUCCESS'
GROUP BY m.id, m.title
ORDER BY booking_count DESC
LIMIT 10;
```

Kỳ vọng:

- Join đi qua `showtimes(movie_id)` và `bookings(showtime_id/status)`.
- Nếu dashboard nặng, tách bảng tổng hợp theo ngày là bước tối ưu tiếp theo.

## 6. Cách đọc EXPLAIN ANALYZE nhanh

Ưu tiên nhìn các dòng:

- `actual time`: thời gian thực tế.
- `rows`: số dòng xử lý.
- `loops`: số lần lặp. Loops quá cao thường là dấu hiệu nested loop tốn kém.
- `Seq Scan`: không luôn xấu. Với bảng nhỏ thì bình thường.
- `Index Scan` hoặc `Bitmap Index Scan`: tốt cho bảng lớn/hot query.
- `Buffers`: nếu đọc rất nhiều shared blocks, query đang chạm nhiều dữ liệu.

## 7. Khi nào cần tách read model dashboard?

Chưa bắt buộc ở quy mô đồ án hoặc dữ liệu vừa. Nên làm khi:

- Dashboard bị gọi liên tục.
- Bảng `payments/bookings/tickets` có hàng triệu dòng.
- Query doanh thu/top phim phải group nhiều bảng và chậm rõ rệt.

Hướng mở rộng chuẩn product:

- Tạo bảng `daily_revenue_summary`.
- Scheduler hoặc event payment success cập nhật số liệu.
- Dashboard đọc bảng summary thay vì join/group bảng giao dịch nóng.

## 8. Checklist kiểm tra sau tối ưu

- Gọi trang chủ nhiều lần: lần sau không tăng SQL với phim/rạp cấu hình.
- Admin sửa phim/rạp/phòng/ghế: dữ liệu frontend cập nhật sau request tiếp theo.
- Đặt vé/giữ ghế: trạng thái ghế realtime vẫn đổi ngay, không bị cache.
- Thanh toán thành công: vé sinh bình thường, email gửi bình thường.
- Mã giảm giá hết lượt: cache khuyến mãi được clear sau payment success.
