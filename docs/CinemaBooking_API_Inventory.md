# CinemaBooking API Inventory

Tài liệu này được trích từ annotation trong `src/main/java/com/cinema/booking/controller`. Source code là căn cứ ưu tiên. Cột frontend được dò từ `cinema-client/src/api/*.ts`; nếu không có kết quả thì không suy diễn rằng API không thể được gọi gián tiếp.

Số endpoint trích được: **101**.

## API CONSISTENCY FINDINGS

- API client frontend tập trung trong `src/api`, giúp đối chiếu dễ hơn. Các URL có nội suy `${...}` được chuẩn hóa thành `{id}` trong quá trình dò.
- Một số endpoint backend là callback/webhook hoặc scheduler support; không có lời gọi trực tiếp từ frontend là có chủ đích, ví dụ VNPay callback, SePay webhook, MoMo IPN.
- `MomoPaymentGateway` tồn tại ở backend nhưng `momo.enabled` mặc định là `false`; frontend không được xem là đang cung cấp cổng MoMo mặc định nếu chưa có cấu hình hợp lệ.
- Có hai route đọc sơ đồ ghế cùng gọi `bookingService.getSeatMap`: `/api/v1/showtimes/{id}/seats` và `/api/v1/bookings/showtimes/{showtimeId}/seats`. React API client gọi route thứ nhất. Source chưa thể hiện annotation deprecation cho route thứ hai; nên chọn một canonical route và có giai đoạn deprecate trước khi xóa để tránh tăng bề mặt API không cần thiết.
- Tương tự, `BookingController` và `TicketController` đều có POST check-in, cùng xác thực QR/cinema/showtime rồi gọi `bookingService.checkInTicket`; React gọi `/api/v1/tickets/check-in`. Nên xem route ở `TicketController` là canonical vì domain rõ ràng hơn và quản lý alias còn lại theo chính sách tương thích API.
- Không phát hiện endpoint React gọi sai HTTP method từ các API client tĩnh đã dò. Các lời gọi động ngoài thư mục `src/api` cần được kiểm tra lại nếu về sau thêm code mới.

## Admin Audit APIs

| Module | HTTP Method | Endpoint | Controller | Method | Request DTO/Params | Response | Authorization | Front-end sử dụng |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| AdminAuditLog | GET | `/api/v1/admin/audit-logs` | AdminAuditLogController | getAuditLogs | Query model: 1 tham số; Pageable | ApiResponse<Page<AdminAuditLogResponse>> | hasAuthority('AUDIT_VIEW') | auditLogApi.ts |

## Analytics APIs

| Module | HTTP Method | Endpoint | Controller | Method | Request DTO/Params | Response | Authorization | Front-end sử dụng |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Analytics | GET | `/api/v1/analytics/summary` | AnalyticsController | getDashboardSummary | Không có body; xem tham số method | ApiResponse<DashboardSummaryResponse> | hasAuthority('ANALYTICS_VIEW') | analyticsApi.ts |
| Analytics | GET | `/api/v1/analytics/revenue/daily` | AnalyticsController | getDailyRevenue | Query: 2 tham số | ApiResponse<List<RevenueByPeriodResponse>> | hasAuthority('ANALYTICS_VIEW') | analyticsApi.ts |
| Analytics | GET | `/api/v1/analytics/revenue/monthly` | AnalyticsController | getMonthlyRevenue | Query: 2 tham số | ApiResponse<List<RevenueByPeriodResponse>> | hasAuthority('ANALYTICS_VIEW') | analyticsApi.ts |
| Analytics | GET | `/api/v1/analytics/movies/top-revenue` | AnalyticsController | getTopMoviesByRevenue | Query: 3 tham số | ApiResponse<List<TopMovieRevenueResponse>> | hasAuthority('ANALYTICS_VIEW') | analyticsApi.ts |
| Analytics | GET | `/api/v1/analytics/showtimes` | AnalyticsController | getShowtimeStats | Query: 3 tham số; Pageable | ApiResponse<Page<ShowtimeStatsResponse>> | hasAuthority('ANALYTICS_VIEW') | analyticsApi.ts |
| Analytics | GET | `/api/v1/analytics/revenue/export` | AnalyticsController | exportRevenueCsv | Query: 4 tham số | ResponseEntity<byte[]> | hasAuthority('ANALYTICS_VIEW') | analyticsApi.ts |

## Authentication Audit APIs

| Module | HTTP Method | Endpoint | Controller | Method | Request DTO/Params | Response | Authorization | Front-end sử dụng |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| AuthAuditLog | GET | `/api/v1/admin/auth-audit-logs` | AuthAuditLogController | getAuthAuditLogs | Query model: 1 tham số; Pageable | ApiResponse<Page<AuthAuditLogResponse>> | hasAuthority('AUDIT_VIEW') | Không tìm thấy lời gọi trực tiếp từ frontend/api |

## Authentication APIs

| Module | HTTP Method | Endpoint | Controller | Method | Request DTO/Params | Response | Authorization | Front-end sử dụng |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Authentication | POST | `/auth/token` | AuthenticationController | authenticate | Body: AuthenticationRequest; Authentication context; HTTP request context | ApiResponse<AuthenticationResponse> | PUBLIC (SecurityConfig) | authApi.ts |
| Authentication | POST | `/auth/google` | AuthenticationController | authenticateWithGoogle | Body: GoogleLoginRequest; HTTP request context | ApiResponse<AuthenticationResponse> | PUBLIC (SecurityConfig) | authApi.ts |
| Authentication | POST | `/auth/introspect` | AuthenticationController | introspect | Body: IntrospectRequest | ApiResponse<IntrospectResponse> | PUBLIC (SecurityConfig) | Không tìm thấy lời gọi trực tiếp từ frontend/api |
| Authentication | POST | `/auth/refresh` | AuthenticationController | authenticate | Body: RefreshRequest; HTTP request context | ApiResponse<AuthenticationResponse> | PUBLIC (SecurityConfig) | authApi.ts |
| Authentication | POST | `/auth/logout` | AuthenticationController | logout | Body: LogoutRequest; HTTP request context | ApiResponse<Void> | PUBLIC (SecurityConfig) | authApi.ts |
| Authentication | GET | `/auth/sessions` | AuthenticationController | getSessions | Authentication context; HTTP request context | ApiResponse<List<AuthSessionResponse>> | Authenticated by SecurityConfig; scope may be enforced in service | authApi.ts |
| Authentication | DELETE | `/auth/sessions/{sessionId}` | AuthenticationController | revokeSession | Path: 1 tham số; Authentication context; HTTP request context | ApiResponse<Void> | Authenticated by SecurityConfig; scope may be enforced in service | Không tìm thấy lời gọi trực tiếp từ frontend/api |
| Authentication | DELETE | `/auth/sessions/others` | AuthenticationController | revokeOtherSessions | Authentication context; HTTP request context | ApiResponse<Void> | Authenticated by SecurityConfig; scope may be enforced in service | authApi.ts |

## Booking APIs

| Module | HTTP Method | Endpoint | Controller | Method | Request DTO/Params | Response | Authorization | Front-end sử dụng |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Booking | GET | `/api/v1/bookings/showtimes/{showtimeId}/seats` | BookingController | getSeatMap | Path: 1 tham số | ApiResponse<List<SeatMapItemResponse>> | PUBLIC (SecurityConfig) | Không tìm thấy lời gọi trực tiếp từ frontend/api |
| Booking | POST | `/api/v1/bookings/hold` | BookingController | holdSeats | Body: HoldSeatRequest; HTTP request context | ApiResponse<HoldSeatResponse> | hasAuthority('BOOKING_CREATE') | bookingApi.ts |
| Booking | POST | `/api/v1/bookings` | BookingController | createBooking | Body: CreateBookingRequest | ApiResponse<BookingResponse> | hasAuthority('BOOKING_CREATE') | bookingApi.ts |
| Booking | GET | `/api/v1/bookings/my` | BookingController | getMyBookings | Query: 1 tham số; Pageable | ApiResponse<Page<BookingResponse>> | hasAuthority('BOOKING_VIEW_OWN') | bookingApi.ts |
| Booking | GET | `/api/v1/bookings` | BookingController | getAllBookings | Query model: 1 tham số; Pageable | ApiResponse<Page<BookingResponse>> | hasAuthority('BOOKING_VIEW_ALL') | bookingApi.ts |
| Booking | GET | `/api/v1/bookings/{id}` | BookingController | getBookingById | Path: 1 tham số | ApiResponse<BookingResponse> | hasAnyAuthority('BOOKING_VIEW_OWN','BOOKING_VIEW_ALL') | bookingApi.ts |
| Booking | PATCH | `/api/v1/bookings/{id}/cancel` | BookingController | cancelBooking | Path: 1 tham số | ApiResponse<BookingResponse> | hasAnyAuthority('BOOKING_CANCEL_OWN','BOOKING_CANCEL_ALL') | bookingApi.ts |
| Booking | PATCH | `/api/v1/bookings/{id}/promotion` | BookingController | applyPromotion | Body: ApplyPromotionRequest; Path: 1 tham số | ApiResponse<BookingResponse> | hasAuthority('BOOKING_VIEW_OWN') | bookingApi.ts |
| Booking | DELETE | `/api/v1/bookings/{id}/promotion` | BookingController | removePromotion | Path: 1 tham số | ApiResponse<BookingResponse> | hasAuthority('BOOKING_VIEW_OWN') | bookingApi.ts |
| Booking | GET | `/api/v1/bookings/tickets/my` | BookingController | getMyTickets | Pageable | ApiResponse<Page<TicketResponse>> | hasAuthority('TICKET_VIEW_OWN') | Không tìm thấy lời gọi trực tiếp từ frontend/api |
| Booking | POST | `/api/v1/bookings/tickets/check-in` | BookingController | checkIn | Body: TicketCheckInRequest; Query: 3 tham số | ApiResponse<TicketResponse> | hasAuthority('TICKET_CHECKIN') | Không tìm thấy lời gọi trực tiếp từ frontend/api |

## Cinema APIs

| Module | HTTP Method | Endpoint | Controller | Method | Request DTO/Params | Response | Authorization | Front-end sử dụng |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Cinema | POST | `/api/v1/cinemas` | CinemaController | createCinema | Body: CinemaCreationRequest | ApiResponse<CinemaResponse> | hasAuthority('CINEMA_CREATE') | cinemaApi.ts |
| Cinema | GET | `/api/v1/cinemas` | CinemaController | getAllCinemas | Query: 3 tham số; Pageable | ApiResponse<Page<CinemaResponse>> | PUBLIC (SecurityConfig) | cinemaApi.ts |
| Cinema | GET | `/api/v1/cinemas/{id}` | CinemaController | getCinemaById | Path: 1 tham số | ApiResponse<CinemaResponse> | PUBLIC (SecurityConfig) | cinemaApi.ts |
| Cinema | PUT | `/api/v1/cinemas/{id}` | CinemaController | updateCinema | Body: CinemaUpdateRequest; Path: 1 tham số | ApiResponse<CinemaResponse> | hasAuthority('CINEMA_UPDATE') | cinemaApi.ts |
| Cinema | DELETE | `/api/v1/cinemas/{id}` | CinemaController | deleteCinema | Path: 1 tham số | ApiResponse<Void> | hasAuthority('CINEMA_DELETE') | cinemaApi.ts |
| Cinema | GET | `/api/v1/cinemas/map` | CinemaController | getMapData | Không có body; xem tham số method | ApiResponse<List<CinemaMapResponse>> | PUBLIC (SecurityConfig) | cinemaApi.ts |
| Cinema | GET | `/api/v1/cinemas/nearest` | CinemaController | getNearestCinemas | Query: 3 tham số | ApiResponse<List<CinemaMapResponse>> | PUBLIC (SecurityConfig) | cinemaApi.ts |

## Movie APIs

| Module | HTTP Method | Endpoint | Controller | Method | Request DTO/Params | Response | Authorization | Front-end sử dụng |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Movie | POST | `/api/v1/movies` | MovieController | createMovie | Body: MovieCreationRequest | ApiResponse<MovieResponse> | hasAuthority('MOVIE_CREATE') | movieApi.ts |
| Movie | GET | `/api/v1/movies` | MovieController | getAllMovies | Query: 2 tham số; Pageable | ApiResponse<Page<MovieResponse>> | PUBLIC (SecurityConfig) | movieApi.ts |
| Movie | GET | `/api/v1/movies/{id}` | MovieController | getMovieById | Path: 1 tham số | ApiResponse<MovieResponse> | PUBLIC (SecurityConfig) | movieApi.ts |
| Movie | PUT | `/api/v1/movies/{id}` | MovieController | updateMovie | Body: MovieUpdateRequest; Path: 1 tham số | ApiResponse<MovieResponse> | hasAuthority('MOVIE_UPDATE') | movieApi.ts |
| Movie | DELETE | `/api/v1/movies/{id}` | MovieController | deleteMovie | Path: 1 tham số | ApiResponse<Void> | hasAuthority('MOVIE_DELETE') | movieApi.ts |

## Payment APIs

| Module | HTTP Method | Endpoint | Controller | Method | Request DTO/Params | Response | Authorization | Front-end sử dụng |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Payment | POST | `/api/v1/payments/initiate` | PaymentController | initiatePayment | Query: 3 tham số; HTTP request context | ApiResponse<String> | hasAuthority('PAYMENT_CREATE') | paymentApi.ts |
| Payment | GET | `/api/v1/payments/my` | PaymentController | getMyPayments | Pageable | ApiResponse<Page<PaymentResponse>> | hasAuthority('PAYMENT_VIEW_OWN') | paymentApi.ts |
| Payment | GET | `/api/v1/payments` | PaymentController | getAllPayments | Query model: 1 tham số; Pageable | ApiResponse<Page<PaymentResponse>> | hasAuthority('PAYMENT_VIEW_ALL') | paymentApi.ts |
| Payment | GET | `/api/v1/payments/events` | PaymentController | getPaymentEvents | Query model: 1 tham số; Pageable | ApiResponse<Page<PaymentEventResponse>> | hasAuthority('PAYMENT_VIEW_ALL') | paymentApi.ts |
| Payment | GET | `/api/v1/payments/events/monitoring-summary` | PaymentController | getPaymentMonitoringSummary | Query: 1 tham số | ApiResponse<PaymentMonitoringSummaryResponse> | hasAuthority('PAYMENT_VIEW_ALL') | paymentApi.ts |
| Payment | GET | `/api/v1/payments/reconciliation` | PaymentController | getReconciliationIssues | Query: 1 tham số | ApiResponse<List<PaymentReconciliationIssueResponse>> | hasAuthority('PAYMENT_VIEW_ALL') | paymentApi.ts |
| Payment | GET | `/api/v1/payments/refunds` | PaymentController | getRefunds | Query model: 1 tham số; Pageable | ApiResponse<Page<RefundResponse>> | hasAuthority('PAYMENT_VIEW_ALL') | paymentApi.ts |
| Payment | POST | `/api/v1/payments/refunds/{refundId}/complete` | PaymentController | completeRefund | Body: RefundCompleteRequest; Path: 1 tham số | ApiResponse<RefundResponse> | hasAuthority('PAYMENT_REFUND') | paymentApi.ts |
| Payment | POST | `/api/v1/payments/refunds/{refundId}/fail` | PaymentController | failRefund | Body: RefundFailRequest; Path: 1 tham số | ApiResponse<RefundResponse> | hasAuthority('PAYMENT_REFUND') | paymentApi.ts |
| Payment | GET | `/api/v1/payments/vnpay-callback` | PaymentController | vnpayCallback | HTTP request context | void | PUBLIC (SecurityConfig) | Không tìm thấy lời gọi trực tiếp từ frontend/api |
| Payment | GET | `/api/v1/payments/momo-return` | PaymentController | momoReturn | HTTP request context | void | PUBLIC (SecurityConfig) | Không tìm thấy lời gọi trực tiếp từ frontend/api |
| Payment | POST | `/api/v1/payments/momo-ipn` | PaymentController | momoIpn | Body: Map | Map<String, Object> | PUBLIC (SecurityConfig) | Không tìm thấy lời gọi trực tiếp từ frontend/api |
| Payment | POST | `/api/v1/payments/sepay-webhook` | PaymentController | sePayWebhook | Body: String; HTTP request context | Map<String, Object> | PUBLIC (SecurityConfig) | Không tìm thấy lời gọi trực tiếp từ frontend/api |

## Promotion APIs

| Module | HTTP Method | Endpoint | Controller | Method | Request DTO/Params | Response | Authorization | Front-end sử dụng |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Promotion | POST | `/api/v1/promotions` | PromotionController | createPromotion | Body: PromotionCreationRequest | ApiResponse<PromotionResponse> | hasAuthority('PROMOTION_CREATE') | promotionApi.ts |
| Promotion | GET | `/api/v1/promotions/{id}` | PromotionController | getPromotionById | Path: 1 tham số | ApiResponse<PromotionResponse> | hasAuthority('PROMOTION_VIEW') | promotionApi.ts |
| Promotion | GET | `/api/v1/promotions/code/{code}` | PromotionController | getPromotionByCode | Path: 1 tham số | ApiResponse<PromotionResponse> | hasAuthority('PROMOTION_VIEW') | Không tìm thấy lời gọi trực tiếp từ frontend/api |
| Promotion | GET | `/api/v1/promotions` | PromotionController | getAllPromotions | Query: 2 tham số; Pageable | ApiResponse<Page<PromotionResponse>> | hasAuthority('PROMOTION_VIEW') | promotionApi.ts |
| Promotion | GET | `/api/v1/promotions/available` | PromotionController | getAvailablePromotions | Pageable | ApiResponse<Page<PromotionResponse>> | hasAuthority('PROMOTION_VIEW') | Không tìm thấy lời gọi trực tiếp từ frontend/api |
| Promotion | PUT | `/api/v1/promotions/{id}` | PromotionController | updatePromotion | Body: PromotionUpdateRequest; Path: 1 tham số | ApiResponse<PromotionResponse> | hasAuthority('PROMOTION_UPDATE') | promotionApi.ts |
| Promotion | DELETE | `/api/v1/promotions/{id}` | PromotionController | deletePromotion | Path: 1 tham số | ApiResponse<Void> | hasAuthority('PROMOTION_DELETE') | promotionApi.ts |

## Room APIs

| Module | HTTP Method | Endpoint | Controller | Method | Request DTO/Params | Response | Authorization | Front-end sử dụng |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Room | POST | `/api/v1/rooms` | RoomController | createRoom | Body: RoomCreationRequest | ApiResponse<RoomResponse> | hasAuthority('ROOM_CREATE') | roomSeatApi.ts |
| Room | GET | `/api/v1/rooms/cinema/{cinemaId}` | RoomController | getRoomsByCinemaId | Path: 1 tham số | ApiResponse<List<RoomResponse>> | hasAuthority('ROOM_VIEW') | roomSeatApi.ts |
| Room | GET | `/api/v1/rooms/{id}` | RoomController | getRoomById | Path: 1 tham số | ApiResponse<RoomResponse> | hasAuthority('ROOM_VIEW') | roomSeatApi.ts |
| Room | PUT | `/api/v1/rooms/{id}` | RoomController | updateRoom | Body: RoomUpdateRequest; Path: 1 tham số | ApiResponse<RoomResponse> | hasAuthority('ROOM_UPDATE') | roomSeatApi.ts |
| Room | DELETE | `/api/v1/rooms/{id}` | RoomController | deleteRoom | Path: 1 tham số | ApiResponse<Void> | hasAuthority('ROOM_DELETE') | roomSeatApi.ts |

## Seat APIs

| Module | HTTP Method | Endpoint | Controller | Method | Request DTO/Params | Response | Authorization | Front-end sử dụng |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Seat | POST | `/api/v1/seats` | SeatController | createSeat | Body: SeatCreationRequest | ApiResponse<SeatResponse> | hasAuthority('SEAT_CREATE') | Không tìm thấy lời gọi trực tiếp từ frontend/api |
| Seat | POST | `/api/v1/seats/bulk-generate` | SeatController | bulkGenerateSeats | Body: SeatBulkGenerateRequest | ApiResponse<SeatBulkGenerateResponse> | hasAuthority('SEAT_CREATE') | roomSeatApi.ts |
| Seat | GET | `/api/v1/seats/room/{roomId}` | SeatController | getSeatsByRoom | Path: 1 tham số | ApiResponse<List<SeatResponse>> | hasAuthority('SEAT_VIEW') | roomSeatApi.ts |
| Seat | GET | `/api/v1/seats/{id}` | SeatController | getSeatById | Path: 1 tham số | ApiResponse<SeatResponse> | hasAuthority('SEAT_VIEW') | roomSeatApi.ts |
| Seat | PUT | `/api/v1/seats/{id}` | SeatController | updateSeat | Body: SeatUpdateRequest; Path: 1 tham số | ApiResponse<SeatResponse> | hasAuthority('SEAT_UPDATE') | roomSeatApi.ts |
| Seat | DELETE | `/api/v1/seats/{id}` | SeatController | deleteSeat | Path: 1 tham số | ApiResponse<Void> | hasAuthority('SEAT_DELETE') | roomSeatApi.ts |

## Showtime APIs

| Module | HTTP Method | Endpoint | Controller | Method | Request DTO/Params | Response | Authorization | Front-end sử dụng |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Showtime | GET | `/api/v1/showtimes/home` | ShowtimeController | getHomeShowtimes | Query: 5 tham số | ApiResponse<HomeShowtimeFeedResponse> | PUBLIC (SecurityConfig) | showtimeApi.ts |
| Showtime | POST | `/api/v1/showtimes` | ShowtimeController | createShowtime | Body: ShowtimeCreationRequest | ApiResponse<ShowtimeResponse> | hasAuthority('SHOWTIME_CREATE') | Không tìm thấy lời gọi trực tiếp từ frontend/api |
| Showtime | GET | `/api/v1/showtimes/{id}` | ShowtimeController | getShowtimeById | Path: 1 tham số | ApiResponse<ShowtimeResponse> | PUBLIC (SecurityConfig) | Không tìm thấy lời gọi trực tiếp từ frontend/api |
| Showtime | GET | `/api/v1/showtimes/{id}/seats` | ShowtimeController | getSeatMap | Path: 1 tham số | ApiResponse<List<SeatMapItemResponse>> | PUBLIC (SecurityConfig) | bookingApi.ts |
| Showtime | GET | `/api/v1/showtimes` | ShowtimeController | getAllShowtimes | Query model: 1 tham số; Pageable | ApiResponse<Page<ShowtimeResponse>> | PUBLIC (SecurityConfig) | Không tìm thấy lời gọi trực tiếp từ frontend/api |
| Showtime | GET | `/api/v1/showtimes/movie/{movieId}` | ShowtimeController | getShowtimesByMovieId | Path: 1 tham số | ApiResponse<List<ShowtimeResponse>> | PUBLIC (SecurityConfig) | movieApi.ts |
| Showtime | GET | `/api/v1/showtimes/cinema/{cinemaId}` | ShowtimeController | getShowtimesByCinemaId | Path: 1 tham số; Pageable | ApiResponse<Page<ShowtimeResponse>> | PUBLIC (SecurityConfig) | cinemaApi.ts |
| Showtime | PUT | `/api/v1/showtimes/{id}` | ShowtimeController | updateShowtime | Body: ShowtimeUpdateRequest; Path: 1 tham số | ApiResponse<ShowtimeResponse> | hasAuthority('SHOWTIME_UPDATE') | Không tìm thấy lời gọi trực tiếp từ frontend/api |
| Showtime | DELETE | `/api/v1/showtimes/{id}` | ShowtimeController | deleteShowtime | Path: 1 tham số | ApiResponse<Void> | hasAuthority('SHOWTIME_DELETE') | Không tìm thấy lời gọi trực tiếp từ frontend/api |
| Showtime | POST | `/api/v1/showtimes/{id}/cancel` | ShowtimeController | cancelShowtime | Body: ShowtimeCancelRequest; Path: 1 tham số | ApiResponse<ShowtimeResponse> | hasAuthority('SHOWTIME_UPDATE') | Không tìm thấy lời gọi trực tiếp từ frontend/api |

## Ticket APIs

| Module | HTTP Method | Endpoint | Controller | Method | Request DTO/Params | Response | Authorization | Front-end sử dụng |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Ticket | GET | `/api/v1/tickets/my` | TicketController | getMyTickets | Pageable | ApiResponse<Page<TicketResponse>> | hasAuthority('TICKET_VIEW_OWN') | ticketApi.ts |
| Ticket | POST | `/api/v1/tickets/check-in` | TicketController | checkInTicket | Body: TicketCheckInRequest; Query: 3 tham số | ApiResponse<TicketResponse> | hasAuthority('TICKET_CHECKIN') | ticketApi.ts |
| Ticket | GET | `/api/v1/tickets/check-in/showtimes` | TicketController | getOpenCheckInShowtimes | Query: 1 tham số | ApiResponse<List<ShowtimeResponse>> | hasAuthority('TICKET_CHECKIN') | ticketApi.ts |
| Ticket | GET | `/api/v1/tickets` | TicketController | getAllTickets | Pageable | ApiResponse<Page<TicketResponse>> | hasAuthority('TICKET_VIEW_ALL') | ticketApi.ts |

## User APIs

| Module | HTTP Method | Endpoint | Controller | Method | Request DTO/Params | Response | Authorization | Front-end sử dụng |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| User | POST | `/api/v1/users/register` | UserController | register | Body: UserCreationRequest | ApiResponse<UserResponse> | PUBLIC (SecurityConfig) | authApi.ts |
| User | POST | `/api/v1/users/verify-email` | UserController | verifyEmail | Body: EmailVerificationRequest | ApiResponse<Void> | PUBLIC (SecurityConfig) | authApi.ts |
| User | POST | `/api/v1/users/resend-verification` | UserController | resendEmailVerification | Body: ResendEmailVerificationRequest | ApiResponse<Void> | PUBLIC (SecurityConfig) | authApi.ts, userApi.ts |
| User | POST | `/api/v1/users/forgot-password` | UserController | forgotPassword | Body: ForgotPasswordRequest | ApiResponse<Void> | PUBLIC (SecurityConfig) | authApi.ts |
| User | POST | `/api/v1/users/reset-password` | UserController | resetPassword | Body: ResetPasswordRequest | ApiResponse<Void> | PUBLIC (SecurityConfig) | authApi.ts |
| User | POST | `/api/v1/users` | UserController | createUser | Body: UserCreationRequest | ApiResponse<UserResponse> | hasAuthority('USER_CREATE') | userApi.ts |
| User | GET | `/api/v1/users` | UserController | getAllUsers | Query: 5 tham số; Pageable | ApiResponse<Page<UserResponse>> | hasAuthority('USER_VIEW') | userApi.ts |
| User | GET | `/api/v1/users/roles` | UserController | getAllRoles | Không có body; xem tham số method | ApiResponse<List<RoleResponse>> | hasAuthority('USER_VIEW') | userApi.ts |
| User | GET | `/api/v1/users/{id}` | UserController | getUserById | Path: 1 tham số | ApiResponse<UserResponse> | hasAuthority('USER_VIEW') | userApi.ts |
| User | PUT | `/api/v1/users/{id}` | UserController | updateUser | Body: UserUpdateRequest; Path: 1 tham số | ApiResponse<UserResponse> | hasAuthority('USER_UPDATE') | userApi.ts |
| User | DELETE | `/api/v1/users/{id}` | UserController | deleteUser | Path: 1 tham số | ApiResponse<Void> | hasAuthority('USER_DELETE') | userApi.ts |
| User | PATCH | `/api/v1/users/{id}/block` | UserController | blockUser | Path: 1 tham số; Authentication context | ApiResponse<UserResponse> | hasAuthority('USER_BLOCK') | userApi.ts |
| User | PATCH | `/api/v1/users/{id}/unblock` | UserController | unblockUser | Path: 1 tham số | ApiResponse<UserResponse> | hasAuthority('USER_BLOCK') | userApi.ts |
| User | POST | `/api/v1/users/{id}/password-reset` | UserController | requestPasswordResetByAdmin | Path: 1 tham số | ApiResponse<Void> | hasAuthority('USER_UPDATE') | userApi.ts |
| User | GET | `/api/v1/users/me` | UserController | getMyProfile | Không có body; xem tham số method | ApiResponse<UserResponse> | Authenticated by SecurityConfig; scope may be enforced in service | authApi.ts, userApi.ts |
| User | PATCH | `/api/v1/users/me` | UserController | updateMyProfile | Body: UserUpdateRequest; Authentication context | ApiResponse<UserResponse> | hasAuthority('PROFILE_UPDATE') | authApi.ts, userApi.ts |
| User | PATCH | `/api/v1/users/me/password` | UserController | changeMyPassword | Body: ChangePasswordRequest; Authentication context | ApiResponse<Void> | hasAuthority('PROFILE_UPDATE') | userApi.ts |
