CREATE TABLE IF NOT EXISTS payment_groups (
    payment_group_id     VARCHAR(36) NOT NULL PRIMARY KEY,
    payment_method       VARCHAR(20) NOT NULL,
    payment_status       VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    amount               DOUBLE      NOT NULL,
    vnpay_transaction_id VARCHAR(255),
    vnpay_order_info     VARCHAR(255),
    created_at           DATETIME,
    updated_at           DATETIME,
    is_active            TINYINT(1)  NOT NULL DEFAULT 1,
    paid_at              DATETIME
);

ALTER TABLE payments
    ADD COLUMN payment_group_id VARCHAR(36) NULL,
    ADD CONSTRAINT fk_payment_group
        FOREIGN KEY (payment_group_id) REFERENCES payment_groups (payment_group_id);
