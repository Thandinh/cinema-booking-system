CREATE TABLE IF NOT EXISTS payment_events (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    payment_id UUID,
    booking_id UUID,
    method VARCHAR(50),
    transaction_no VARCHAR(255),
    event_type VARCHAR(80) NOT NULL,
    payment_status_before VARCHAR(20),
    payment_status_after VARCHAR(20),
    booking_status_before VARCHAR(20),
    booking_status_after VARCHAR(20),
    success BOOLEAN,
    message VARCHAR(1000),
    payload JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

DROP TRIGGER IF EXISTS update_payment_events_modtime ON payment_events;
CREATE TRIGGER update_payment_events_modtime
    BEFORE UPDATE ON payment_events
    FOR EACH ROW
    EXECUTE PROCEDURE update_modified_column();

CREATE INDEX IF NOT EXISTS idx_payment_events_created_at
    ON payment_events(created_at DESC);

CREATE INDEX IF NOT EXISTS idx_payment_events_booking_created_at
    ON payment_events(booking_id, created_at DESC)
    WHERE booking_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_payment_events_payment_created_at
    ON payment_events(payment_id, created_at DESC)
    WHERE payment_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_payment_events_transaction_no
    ON payment_events(transaction_no)
    WHERE transaction_no IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_payment_events_type_created_at
    ON payment_events(event_type, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_payment_events_success_created_at
    ON payment_events(success, created_at DESC)
    WHERE success IS NOT NULL;
