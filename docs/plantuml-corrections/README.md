# PlantUML Corrections

Thu muc nay chua cac ban PlantUML da chinh lai cho nhung so do can sua de sat hon voi he thong hien tai.

Nguyen tac chinh:

- Khong dung `REFUND_PENDING` nhu mot trang thai luu trong database vi code hien tai khong co enum/trang thai nay.
- Luong huy suat chieu dung voi code: `booking = CANCELLED`, `ticket = CANCELLED`, ghe tra ve `AVAILABLE`, va yeu cau hoan tien duoc ghi nhan qua `PaymentEventType.REFUND_REQUESTED`.
- `BookingDetail` dung field `priceAtBooking`, khop entity va cot `price_at_booking`.
- Mail nen trinh bay la `Mail Service` hoac `Mail Service Provider`; SMTP chi nen la ghi chu ky thuat.

Danh sach file:

- `Activity_Huy_Suat_Chieu_Va_Hoan_Tien.puml`
- `State_Booking_Payment_Seat_Ticket.puml`
- `Class_Nghiep_Vu_Dat_Ve.puml`
- `Activity_Thanh_Toan.puml`
- `BFD_Gui_Email.puml`
- `BFD_Quan_Ly_Phim.puml`

