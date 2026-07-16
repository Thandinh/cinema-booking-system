-- Runtime-safe schema patches for dev/local databases.
-- Keep Hibernate in validate mode, but let Spring apply small compatible patches first.

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
