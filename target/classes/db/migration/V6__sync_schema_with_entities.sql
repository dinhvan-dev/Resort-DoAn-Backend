-- Sync database schema with the current JPA entities.
-- This keeps the original lower-case table names and adds columns required by BaseEntity/User/Customer/Payment.

ALTER TABLE users
    CHANGE COLUMN createAt created_at DATETIME NULL,
    ADD COLUMN email VARCHAR(255) NULL,
    ADD COLUMN full_name VARCHAR(255) NULL,
    ADD COLUMN phone_number VARCHAR(255) NULL,
    ADD COLUMN updated_at DATETIME NULL,
    ADD COLUMN is_active TINYINT(1) NOT NULL DEFAULT 1;

UPDATE users
SET email = CONCAT(username, '@resort.local')
WHERE email IS NULL OR email = '';

ALTER TABLE users
    MODIFY COLUMN email VARCHAR(255) NOT NULL,
    ADD CONSTRAINT uk_users_email UNIQUE (email);

ALTER TABLE customers
    CHANGE COLUMN createdAt created_at DATETIME NULL,
    ADD COLUMN updated_at DATETIME NULL,
    ADD COLUMN is_active TINYINT(1) NOT NULL DEFAULT 1,
    ADD COLUMN address VARCHAR(255) NULL;

ALTER TABLE bookings
    CHANGE COLUMN createdAt created_at DATETIME NULL,
    ADD COLUMN updated_at DATETIME NULL,
    ADD COLUMN is_active TINYINT(1) NOT NULL DEFAULT 1;

ALTER TABLE payments
    ADD COLUMN updated_at DATETIME NULL,
    ADD COLUMN is_active TINYINT(1) NOT NULL DEFAULT 1;
