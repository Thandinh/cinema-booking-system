-- ========================================================
-- CINEMA BOOKING SYSTEM - FULL DATABASE SETUP
-- ========================================================
-- Use this file only when creating/resetting a database from scratch.
-- Do not auto-run it on every Spring Boot startup because it drops tables.
-- RBAC seed data is managed by ApplicationInitConfig.
-- Mock business data is managed by mock-data.sql.
-- ========================================================

-- 1. CLEAN UP
DROP TABLE IF EXISTS invalidated_token CASCADE;
DROP TABLE IF EXISTS admin_audit_logs CASCADE;
DROP TABLE IF EXISTS tickets CASCADE;
DROP TABLE IF EXISTS payments CASCADE;
DROP TABLE IF EXISTS booking_details CASCADE;
DROP TABLE IF EXISTS bookings CASCADE;
DROP TABLE IF EXISTS seat_status CASCADE;
DROP TABLE IF EXISTS promotions CASCADE;
DROP TABLE IF EXISTS showtimes CASCADE;
DROP TABLE IF EXISTS seats CASCADE;
DROP TABLE IF EXISTS rooms CASCADE;
DROP TABLE IF EXISTS cinemas CASCADE;
DROP TABLE IF EXISTS movies CASCADE;
DROP TABLE IF EXISTS users_roles CASCADE;
DROP TABLE IF EXISTS roles_permissions CASCADE;
DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS permissions CASCADE;
DROP TABLE IF EXISTS roles CASCADE;

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE OR REPLACE FUNCTION update_modified_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

-- =========================================
-- 2. RBAC
-- =========================================

CREATE TABLE roles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(50) UNIQUE NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE permissions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) UNIQUE NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    username VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    dob DATE,
    phone VARCHAR(20),
    email VARCHAR(100) UNIQUE,
    avatar_url VARCHAR(500),
    email_verified BOOLEAN DEFAULT TRUE,
    email_verification_token_hash VARCHAR(64),
    email_verification_expires_at TIMESTAMP,
    password_reset_token_hash VARCHAR(64),
    password_reset_expires_at TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE,
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE users_roles (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE roles_permissions (
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (role_id, permission_id)
);

-- =========================================
-- 3. CINEMA BUSINESS TABLES
-- =========================================

CREATE TABLE movies (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    duration INT,
    genre VARCHAR(100),
    release_date DATE,
    poster_url TEXT,
    status VARCHAR(50),
    director VARCHAR(255),
    actors TEXT,
    language VARCHAR(100),
    subtitle_language VARCHAR(100),
    country VARCHAR(100),
    age_rating VARCHAR(10),
    trailer_url TEXT,
    rating_imdb DECIMAL(3,1) CHECK (rating_imdb IS NULL OR (rating_imdb >= 0 AND rating_imdb <= 10)),
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_movie_status CHECK (status IS NULL OR status IN ('NOW_SHOWING', 'COMING_SOON', 'ENDED'))
);

CREATE TABLE cinemas (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    address TEXT,
    city VARCHAR(100),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    is_active BOOLEAN DEFAULT TRUE,
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE rooms (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    cinema_id UUID NOT NULL REFERENCES cinemas(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE seats (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    room_id UUID NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    row_label VARCHAR(10) NOT NULL,
    seat_number INT NOT NULL,
    seat_type VARCHAR(20) DEFAULT 'NORMAL',
    price_multiplier DECIMAL(5,2) DEFAULT 1.0 CHECK (price_multiplier >= 0),
    row_index INT,
    col_index INT,
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_seat UNIQUE(room_id, row_label, seat_number),
    CONSTRAINT chk_seat_type CHECK (seat_type IN ('NORMAL', 'VIP', 'COUPLE'))
);

CREATE TABLE showtimes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    movie_id UUID NOT NULL REFERENCES movies(id),
    room_id UUID NOT NULL REFERENCES rooms(id),
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    base_price DECIMAL(10,2) NOT NULL CHECK (base_price > 0),
    status VARCHAR(20) DEFAULT 'UPCOMING',
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_showtime_time CHECK (end_time > start_time),
    CONSTRAINT chk_showtime_status CHECK (status IN ('UPCOMING', 'ONGOING', 'ENDED', 'CANCELLED'))
);

CREATE TABLE promotions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    code VARCHAR(50) UNIQUE NOT NULL,
    description TEXT,
    discount_type VARCHAR(20) DEFAULT 'PERCENT',
    discount_value DECIMAL(10,2) NOT NULL CHECK (discount_value >= 0),
    max_discount_amount DECIMAL(10,2),
    min_order_value DECIMAL(10,2) DEFAULT 0,
    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP NOT NULL,
    usage_limit INT,
    used_count INT DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_promotion_discount_type CHECK (discount_type IN ('PERCENT', 'FIXED')),
    CONSTRAINT chk_promotion_dates CHECK (end_date > start_date),
    CONSTRAINT chk_promotion_usage CHECK (usage_limit IS NULL OR usage_limit >= 0),
    CONSTRAINT chk_promotion_used_count CHECK (used_count >= 0)
);

CREATE TABLE seat_status (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    seat_id UUID NOT NULL REFERENCES seats(id) ON DELETE CASCADE,
    showtime_id UUID NOT NULL REFERENCES showtimes(id) ON DELETE CASCADE,
    status VARCHAR(20) DEFAULT 'AVAILABLE',
    hold_by UUID REFERENCES users(id) ON DELETE SET NULL,
    hold_until TIMESTAMP,
    version INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_seat_showtime UNIQUE (seat_id, showtime_id),
    CONSTRAINT chk_seat_status CHECK (status IN ('AVAILABLE', 'HOLD', 'BOOKED')),
    CONSTRAINT chk_seat_status_hold CHECK (
        (status = 'HOLD' AND hold_by IS NOT NULL AND hold_until IS NOT NULL)
        OR (status <> 'HOLD')
    )
);

CREATE TABLE bookings (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id),
    showtime_id UUID NOT NULL REFERENCES showtimes(id),
    promotion_id UUID REFERENCES promotions(id) ON DELETE SET NULL,
    total_price DECIMAL(10,2) NOT NULL CHECK (total_price >= 0),
    discount_amount DECIMAL(10,2) DEFAULT 0 CHECK (discount_amount >= 0),
    status VARCHAR(20) DEFAULT 'PENDING',
    secure_token VARCHAR(255) UNIQUE NOT NULL,
    payment_expires_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_booking_status CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED', 'CANCELLED', 'EXPIRED'))
);

CREATE TABLE booking_details (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    booking_id UUID NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
    seat_id UUID NOT NULL REFERENCES seats(id),
    price_at_booking DECIMAL(10,2) NOT NULL CHECK (price_at_booking >= 0),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE payments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    booking_id UUID REFERENCES bookings(id) ON DELETE SET NULL,
    amount DECIMAL(10,2) CHECK (amount IS NULL OR amount > 0),
    method VARCHAR(50),
    transaction_no VARCHAR(255),
    status VARCHAR(20) DEFAULT 'PENDING',
    provider_response JSONB,
    payment_time TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_payment_method CHECK (method IS NULL OR method IN ('VNPAY', 'MOMO', 'CREDIT_CARD', 'CASH')),
    CONSTRAINT chk_payment_status CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED', 'EXPIRED'))
);

CREATE TABLE tickets (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    booking_detail_id UUID NOT NULL REFERENCES booking_details(id) ON DELETE CASCADE,
    qr_code VARCHAR(100) UNIQUE NOT NULL,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    check_in_time TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_ticket_status CHECK (status IN ('ACTIVE', 'USED', 'CANCELLED'))
);

CREATE TABLE admin_audit_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    actor_id UUID,
    actor_username VARCHAR(255),
    http_method VARCHAR(20) NOT NULL,
    action VARCHAR(80) NOT NULL,
    resource VARCHAR(80) NOT NULL,
    resource_id VARCHAR(100),
    request_path VARCHAR(500) NOT NULL,
    query_string VARCHAR(500),
    ip_address VARCHAR(80),
    user_agent VARCHAR(500),
    status_code INT,
    success BOOLEAN,
    error_message VARCHAR(1000),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE invalidated_token (
    id VARCHAR(255) PRIMARY KEY,
    expiry_time TIMESTAMP NOT NULL
);

-- =========================================
-- 4. UPDATED_AT TRIGGERS
-- =========================================

CREATE TRIGGER update_roles_modtime BEFORE UPDATE ON roles FOR EACH ROW EXECUTE PROCEDURE update_modified_column();
CREATE TRIGGER update_permissions_modtime BEFORE UPDATE ON permissions FOR EACH ROW EXECUTE PROCEDURE update_modified_column();
CREATE TRIGGER update_users_modtime BEFORE UPDATE ON users FOR EACH ROW EXECUTE PROCEDURE update_modified_column();
CREATE TRIGGER update_movies_modtime BEFORE UPDATE ON movies FOR EACH ROW EXECUTE PROCEDURE update_modified_column();
CREATE TRIGGER update_cinemas_modtime BEFORE UPDATE ON cinemas FOR EACH ROW EXECUTE PROCEDURE update_modified_column();
CREATE TRIGGER update_rooms_modtime BEFORE UPDATE ON rooms FOR EACH ROW EXECUTE PROCEDURE update_modified_column();
CREATE TRIGGER update_seats_modtime BEFORE UPDATE ON seats FOR EACH ROW EXECUTE PROCEDURE update_modified_column();
CREATE TRIGGER update_showtimes_modtime BEFORE UPDATE ON showtimes FOR EACH ROW EXECUTE PROCEDURE update_modified_column();
CREATE TRIGGER update_promotions_modtime BEFORE UPDATE ON promotions FOR EACH ROW EXECUTE PROCEDURE update_modified_column();
CREATE TRIGGER update_seat_status_modtime BEFORE UPDATE ON seat_status FOR EACH ROW EXECUTE PROCEDURE update_modified_column();
CREATE TRIGGER update_bookings_modtime BEFORE UPDATE ON bookings FOR EACH ROW EXECUTE PROCEDURE update_modified_column();
CREATE TRIGGER update_booking_details_modtime BEFORE UPDATE ON booking_details FOR EACH ROW EXECUTE PROCEDURE update_modified_column();
CREATE TRIGGER update_payments_modtime BEFORE UPDATE ON payments FOR EACH ROW EXECUTE PROCEDURE update_modified_column();
CREATE TRIGGER update_tickets_modtime BEFORE UPDATE ON tickets FOR EACH ROW EXECUTE PROCEDURE update_modified_column();
CREATE TRIGGER update_admin_audit_logs_modtime BEFORE UPDATE ON admin_audit_logs FOR EACH ROW EXECUTE PROCEDURE update_modified_column();

-- =========================================
-- 5. INDEXES
-- =========================================

CREATE UNIQUE INDEX uq_users_email_verification_token_hash
    ON users(email_verification_token_hash)
    WHERE email_verification_token_hash IS NOT NULL;

CREATE UNIQUE INDEX uq_users_password_reset_token_hash
    ON users(password_reset_token_hash)
    WHERE password_reset_token_hash IS NOT NULL;

CREATE INDEX idx_users_is_deleted_created_at
    ON users(is_deleted, created_at DESC);

CREATE INDEX idx_users_roles_user_id
    ON users_roles(user_id);

CREATE INDEX idx_users_roles_role_id
    ON users_roles(role_id);

CREATE INDEX idx_roles_permissions_role_id
    ON roles_permissions(role_id);

CREATE INDEX idx_roles_permissions_permission_id
    ON roles_permissions(permission_id);

CREATE INDEX idx_cinemas_active_city_name
    ON cinemas(is_active, is_deleted, city, name);

CREATE INDEX idx_rooms_cinema_id_is_deleted
    ON rooms(cinema_id, is_deleted);

CREATE UNIQUE INDEX uq_rooms_active_cinema_name
    ON rooms(cinema_id, lower(name))
    WHERE is_deleted = false;

CREATE INDEX idx_seats_room_id_is_deleted
    ON seats(room_id, is_deleted);

CREATE INDEX idx_movies_status_is_deleted
    ON movies(status, is_deleted);

CREATE INDEX idx_promotions_admin_filter
    ON promotions(is_deleted, is_active, start_date, end_date);

CREATE INDEX idx_promotions_usage_limit
    ON promotions(usage_limit, used_count)
    WHERE usage_limit IS NOT NULL AND is_deleted = false;

CREATE INDEX idx_showtimes_movie_start_time
    ON showtimes(movie_id, start_time)
    WHERE is_deleted = false;

CREATE INDEX idx_showtimes_room_time
    ON showtimes(room_id, start_time, end_time)
    WHERE is_deleted = false;

CREATE INDEX idx_showtimes_status_start_time
    ON showtimes(status, start_time)
    WHERE is_deleted = false;

CREATE INDEX idx_seat_status_showtime_status
    ON seat_status(showtime_id, status);

CREATE INDEX idx_seat_status_showtime_seat
    ON seat_status(showtime_id, seat_id);

CREATE INDEX idx_seat_status_hold_until
    ON seat_status(hold_until)
    WHERE status = 'HOLD';

CREATE INDEX idx_seat_status_hold_by
    ON seat_status(hold_by)
    WHERE hold_by IS NOT NULL;

CREATE INDEX idx_seat_status_hold_release
    ON seat_status(showtime_id, hold_by, hold_until)
    WHERE status = 'HOLD';

CREATE INDEX idx_bookings_user_created_at
    ON bookings(user_id, created_at DESC);

CREATE INDEX idx_bookings_user_status_created_at
    ON bookings(user_id, status, created_at DESC);

CREATE INDEX idx_bookings_status_created_at
    ON bookings(status, created_at DESC);

CREATE INDEX idx_bookings_showtime_id
    ON bookings(showtime_id);

CREATE INDEX idx_bookings_success_showtime_id
    ON bookings(showtime_id, id)
    WHERE status = 'SUCCESS';

CREATE INDEX idx_bookings_pending_expires_at
    ON bookings(payment_expires_at)
    WHERE status = 'PENDING';

CREATE INDEX idx_bookings_pending_expires_id
    ON bookings(payment_expires_at, id)
    WHERE status = 'PENDING';

CREATE INDEX idx_booking_details_booking_id
    ON booking_details(booking_id);

CREATE INDEX idx_booking_details_seat_id
    ON booking_details(seat_id);

CREATE INDEX idx_payments_booking_id
    ON payments(booking_id);

CREATE INDEX idx_payments_booking_status
    ON payments(booking_id, status);

CREATE INDEX idx_payments_status_payment_time
    ON payments(status, payment_time DESC);

CREATE UNIQUE INDEX uq_payments_transaction_no
    ON payments(transaction_no)
    WHERE transaction_no IS NOT NULL;

CREATE UNIQUE INDEX uq_tickets_booking_detail_id
    ON tickets(booking_detail_id);

CREATE INDEX idx_tickets_status
    ON tickets(status);

CREATE INDEX idx_admin_audit_logs_created_at
    ON admin_audit_logs(created_at DESC);

CREATE INDEX idx_admin_audit_logs_resource_created_at
    ON admin_audit_logs(resource, created_at DESC);

CREATE INDEX idx_admin_audit_logs_actor_created_at
    ON admin_audit_logs(actor_id, created_at DESC)
    WHERE actor_id IS NOT NULL;

CREATE INDEX idx_admin_audit_logs_success_created_at
    ON admin_audit_logs(success, created_at DESC);
