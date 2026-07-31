CREATE INDEX IF NOT EXISTS idx_showtimes_status_end_time
    ON showtimes(status, end_time)
    WHERE is_deleted = false;
