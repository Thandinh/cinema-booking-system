UPDATE payment_events
SET event_type = 'PAYMENT_SUCCESS',
    updated_at = CURRENT_TIMESTAMP
WHERE event_type = 'PAYMENT_CONFIRMED';
