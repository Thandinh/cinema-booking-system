-- Runtime-safe schema patches for dev/local databases.
-- Keep Hibernate in validate mode, but let Spring apply small compatible patches first.

ALTER TABLE IF EXISTS users
    ADD COLUMN IF NOT EXISTS avatar_url VARCHAR(500);

ALTER TABLE IF EXISTS users
    ADD COLUMN IF NOT EXISTS email_verified BOOLEAN DEFAULT TRUE;

ALTER TABLE IF EXISTS users
    ADD COLUMN IF NOT EXISTS email_verification_token_hash VARCHAR(64);

ALTER TABLE IF EXISTS users
    ADD COLUMN IF NOT EXISTS email_verification_expires_at TIMESTAMP;

UPDATE users
SET email_verified = TRUE
WHERE email_verified IS NULL;

ALTER TABLE IF EXISTS bookings
    ADD COLUMN IF NOT EXISTS payment_expires_at TIMESTAMP;

ALTER TABLE IF EXISTS bookings
    DROP CONSTRAINT IF EXISTS chk_booking_status;

ALTER TABLE IF EXISTS bookings
    ADD CONSTRAINT chk_booking_status
    CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED', 'CANCELLED', 'EXPIRED'));

ALTER TABLE IF EXISTS payments
    DROP CONSTRAINT IF EXISTS chk_payment_status;

ALTER TABLE IF EXISTS payments
    ADD CONSTRAINT chk_payment_status
    CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED', 'EXPIRED'));

-- Product-grade indexes for the main read/write paths.
-- PostgreSQL does not automatically index foreign keys, so keep these explicit.

CREATE UNIQUE INDEX IF NOT EXISTS uq_users_email_verification_token_hash
    ON users(email_verification_token_hash)
    WHERE email_verification_token_hash IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_users_is_deleted_created_at
    ON users(is_deleted, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_users_roles_user_id
    ON users_roles(user_id);

CREATE INDEX IF NOT EXISTS idx_users_roles_role_id
    ON users_roles(role_id);

CREATE INDEX IF NOT EXISTS idx_roles_permissions_role_id
    ON roles_permissions(role_id);

CREATE INDEX IF NOT EXISTS idx_roles_permissions_permission_id
    ON roles_permissions(permission_id);

CREATE INDEX IF NOT EXISTS idx_cinemas_active_city_name
    ON cinemas(is_active, is_deleted, city, name);

CREATE INDEX IF NOT EXISTS idx_rooms_cinema_id_is_deleted
    ON rooms(cinema_id, is_deleted);

CREATE UNIQUE INDEX IF NOT EXISTS uq_rooms_active_cinema_name
    ON rooms(cinema_id, lower(name))
    WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_seats_room_id_is_deleted
    ON seats(room_id, is_deleted);

CREATE INDEX IF NOT EXISTS idx_movies_status_is_deleted
    ON movies(status, is_deleted);

CREATE INDEX IF NOT EXISTS idx_showtimes_movie_start_time
    ON showtimes(movie_id, start_time)
    WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_showtimes_room_time
    ON showtimes(room_id, start_time, end_time)
    WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_showtimes_status_start_time
    ON showtimes(status, start_time)
    WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_seat_status_showtime_status
    ON seat_status(showtime_id, status);

CREATE INDEX IF NOT EXISTS idx_seat_status_showtime_seat
    ON seat_status(showtime_id, seat_id);

CREATE INDEX IF NOT EXISTS idx_seat_status_hold_until
    ON seat_status(hold_until)
    WHERE status = 'HOLD';

CREATE INDEX IF NOT EXISTS idx_seat_status_hold_by
    ON seat_status(hold_by)
    WHERE hold_by IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_seat_status_hold_release
    ON seat_status(showtime_id, hold_by, hold_until)
    WHERE status = 'HOLD';

CREATE INDEX IF NOT EXISTS idx_bookings_user_created_at
    ON bookings(user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_bookings_status_created_at
    ON bookings(status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_bookings_showtime_id
    ON bookings(showtime_id);

CREATE INDEX IF NOT EXISTS idx_bookings_success_showtime_id
    ON bookings(showtime_id, id)
    WHERE status = 'SUCCESS';

CREATE INDEX IF NOT EXISTS idx_bookings_pending_expires_at
    ON bookings(payment_expires_at)
    WHERE status = 'PENDING';

CREATE INDEX IF NOT EXISTS idx_bookings_pending_expires_id
    ON bookings(payment_expires_at, id)
    WHERE status = 'PENDING';

CREATE INDEX IF NOT EXISTS idx_booking_details_booking_id
    ON booking_details(booking_id);

CREATE INDEX IF NOT EXISTS idx_booking_details_seat_id
    ON booking_details(seat_id);

CREATE INDEX IF NOT EXISTS idx_payments_booking_id
    ON payments(booking_id);

CREATE INDEX IF NOT EXISTS idx_payments_booking_status
    ON payments(booking_id, status);

CREATE INDEX IF NOT EXISTS idx_payments_status_payment_time
    ON payments(status, payment_time DESC);

CREATE UNIQUE INDEX IF NOT EXISTS uq_payments_transaction_no
    ON payments(transaction_no)
    WHERE transaction_no IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_tickets_booking_detail_id
    ON tickets(booking_detail_id);

CREATE INDEX IF NOT EXISTS idx_tickets_status
    ON tickets(status);
