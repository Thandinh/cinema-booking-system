CREATE TABLE IF NOT EXISTS staff_cinemas (
    staff_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    cinema_id UUID NOT NULL REFERENCES cinemas(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (staff_id, cinema_id)
);

CREATE INDEX IF NOT EXISTS idx_staff_cinemas_staff_id
    ON staff_cinemas(staff_id);

CREATE INDEX IF NOT EXISTS idx_staff_cinemas_cinema_id
    ON staff_cinemas(cinema_id);
