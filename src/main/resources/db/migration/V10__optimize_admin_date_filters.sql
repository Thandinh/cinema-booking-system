CREATE INDEX IF NOT EXISTS idx_bookings_showtime_created_at
    ON bookings(showtime_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_payments_created_at
    ON payments(created_at DESC);

CREATE INDEX IF NOT EXISTS idx_payments_status_method_created_at
    ON payments(status, method, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_admin_audit_logs_action_created_at
    ON admin_audit_logs(action, created_at DESC);
