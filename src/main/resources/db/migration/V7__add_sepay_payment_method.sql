ALTER TABLE payments
    DROP CONSTRAINT IF EXISTS chk_payment_method;

ALTER TABLE payments
    ADD CONSTRAINT chk_payment_method
    CHECK (method IS NULL OR method IN ('VNPAY', 'MOMO', 'SEPAY', 'CREDIT_CARD', 'CASH'));
