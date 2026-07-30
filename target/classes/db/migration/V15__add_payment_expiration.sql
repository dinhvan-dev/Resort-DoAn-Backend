ALTER TABLE payments
    ADD COLUMN payment_expired_at DATETIME NULL;

ALTER TABLE payment_groups
    ADD COLUMN payment_expired_at DATETIME NULL;
