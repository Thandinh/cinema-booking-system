-- Keep at most one active checkout for a user and showtime.
-- Older duplicate rows are terminalized before adding the partial unique index.
WITH ranked_pending_bookings AS (
    SELECT id,
           row_number() OVER (
               PARTITION BY user_id, showtime_id
               ORDER BY created_at DESC, id DESC
           ) AS row_number
    FROM bookings
    WHERE status = 'PENDING'
)
UPDATE bookings b
SET status = 'EXPIRED',
    updated_at = CURRENT_TIMESTAMP
FROM ranked_pending_bookings ranked
WHERE b.id = ranked.id
  AND ranked.row_number > 1;

UPDATE payments p
SET status = 'EXPIRED',
    updated_at = CURRENT_TIMESTAMP
WHERE p.status = 'PENDING'
  AND p.booking_id IN (
      SELECT id
      FROM bookings
      WHERE status = 'EXPIRED'
  );

CREATE UNIQUE INDEX IF NOT EXISTS uq_bookings_pending_user_showtime
    ON bookings(user_id, showtime_id)
    WHERE status = 'PENDING';

-- A booking can only have one active payment attempt, regardless of gateway.
-- This prevents a customer from opening VNPay and SePay for the same order.
WITH ranked_pending_payments AS (
    SELECT id,
           row_number() OVER (
               PARTITION BY booking_id
               ORDER BY created_at DESC, id DESC
           ) AS row_number
    FROM payments
    WHERE status = 'PENDING'
      AND booking_id IS NOT NULL
)
UPDATE payments p
SET status = 'EXPIRED',
    updated_at = CURRENT_TIMESTAMP
FROM ranked_pending_payments ranked
WHERE p.id = ranked.id
  AND ranked.row_number > 1;

DROP INDEX IF EXISTS uq_payments_pending_booking_method;

CREATE UNIQUE INDEX IF NOT EXISTS uq_payments_pending_booking
    ON payments(booking_id)
    WHERE status = 'PENDING'
      AND booking_id IS NOT NULL;

-- Promotion usage is reserved while a booking is pending, then released on
-- failure/cancellation/expiry. This makes usage_limit safe under concurrency.
ALTER TABLE bookings
    ADD COLUMN IF NOT EXISTS promotion_reserved BOOLEAN NOT NULL DEFAULT FALSE;

-- Incrementing auth_version invalidates previously issued access and refresh
-- tokens after a password or account-security change.
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS auth_version INTEGER NOT NULL DEFAULT 0;

UPDATE bookings
SET promotion_reserved = true
WHERE promotion_id IS NOT NULL
  AND status IN ('PENDING', 'SUCCESS', 'REFUND_PENDING', 'REFUNDED');

UPDATE promotions p
SET used_count = GREATEST(
    COALESCE(p.used_count, 0),
    COALESCE((
        SELECT COUNT(*)
        FROM bookings b
        WHERE b.promotion_id = p.id
          AND b.promotion_reserved = true
    ), 0)
),
updated_at = CURRENT_TIMESTAMP;
