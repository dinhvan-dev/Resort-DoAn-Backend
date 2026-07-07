ALTER TABLE customers
    ADD COLUMN user_id VARCHAR(36) NULL,
    ADD CONSTRAINT uk_customers_user_id UNIQUE (user_id),
    ADD CONSTRAINT fk_customers_user
        FOREIGN KEY (user_id) REFERENCES users(userId);
