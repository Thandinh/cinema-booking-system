UPDATE payments p
SET status = 'EXPIRED',
    updated_at = CURRENT_TIMESTAMP
WHERE p.status = 'PENDING'
  AND p.booking_id IS NOT NULL
  AND p.method IS NOT NULL
  AND p.id NOT IN (
      SELECT latest.id
      FROM (
          SELECT DISTINCT ON (booking_id, method) id
          FROM payments
          WHERE status = 'PENDING'
            AND booking_id IS NOT NULL
            AND method IS NOT NULL
          ORDER BY booking_id, method, created_at DESC, id DESC
      ) latest
  );

CREATE UNIQUE INDEX IF NOT EXISTS uq_payments_pending_booking_method
    ON payments(booking_id, method)
    WHERE status = 'PENDING'
      AND booking_id IS NOT NULL
      AND method IS NOT NULL;
