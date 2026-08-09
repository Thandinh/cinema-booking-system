ALTER TABLE bookings
    DROP CONSTRAINT IF EXISTS chk_booking_status;

ALTER TABLE bookings
    ADD CONSTRAINT chk_booking_status
    CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED', 'CANCELLED', 'EXPIRED', 'REFUND_PENDING', 'REFUNDED'));

ALTER TABLE payments
    DROP CONSTRAINT IF EXISTS chk_payment_status;

ALTER TABLE payments
    ADD CONSTRAINT chk_payment_status
    CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED', 'EXPIRED', 'REFUND_PENDING', 'REFUNDED', 'REFUND_FAILED'));

CREATE TABLE IF NOT EXISTS refunds (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    booking_id UUID NOT NULL REFERENCES bookings(id),
    payment_id UUID NOT NULL REFERENCES payments(id),
    amount DECIMAL(10,2) NOT NULL CHECK (amount > 0),
    method VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reason VARCHAR(500),
    provider_refund_id VARCHAR(255),
    failure_reason VARCHAR(1000),
    provider_response JSONB,
    requested_at TIMESTAMP,
    processed_at TIMESTAMP,
    requested_by UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_refund_method CHECK (method IN ('VNPAY', 'MOMO', 'SEPAY', 'CREDIT_CARD', 'CASH')),
    CONSTRAINT chk_refund_status CHECK (status IN ('PENDING', 'PROCESSING', 'SUCCESS', 'FAILED', 'CANCELLED'))
);

CREATE INDEX IF NOT EXISTS idx_refunds_booking_created_at
    ON refunds(booking_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_refunds_status_created_at
    ON refunds(status, created_at DESC);

CREATE UNIQUE INDEX IF NOT EXISTS uq_refunds_payment_active
    ON refunds(payment_id)
    WHERE status IN ('PENDING', 'PROCESSING', 'SUCCESS');

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_trigger
        WHERE tgname = 'update_refunds_modtime'
    ) THEN
        CREATE TRIGGER update_refunds_modtime
        BEFORE UPDATE ON refunds
        FOR EACH ROW
        EXECUTE PROCEDURE update_modified_column();
    END IF;
END $$;
