CREATE TABLE IF NOT EXISTS payments (
                                        payment_id           VARCHAR(36)  NOT NULL PRIMARY KEY,
    booking_id           BIGINT       NOT NULL UNIQUE,
    payment_method       VARCHAR(20)  NOT NULL,
    payment_status       VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    amount               DOUBLE       NOT NULL,
    vnpay_transaction_id VARCHAR(255),
    vnpay_order_info     VARCHAR(255),
    created_at           DATETIME,
    paid_at              DATETIME,
    CONSTRAINT fk_payment_booking
    FOREIGN KEY (booking_id) REFERENCES bookings (bookingId)
    );
