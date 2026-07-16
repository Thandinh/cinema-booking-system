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
