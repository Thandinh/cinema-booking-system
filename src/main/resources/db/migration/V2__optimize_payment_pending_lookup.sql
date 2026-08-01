CREATE INDEX IF NOT EXISTS idx_payments_pending_reuse
    ON payments(booking_id, method, status, created_at DESC)
    WHERE status = 'PENDING';
