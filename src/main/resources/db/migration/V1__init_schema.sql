CREATE TABLE IF NOT EXISTS users (
                                     userId      VARCHAR(36)  NOT NULL PRIMARY KEY,
    username    VARCHAR(255) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    role        VARCHAR(20)  NOT NULL,
    createAt    DATE         NOT NULL
    );

-- Bảng rooms
CREATE TABLE IF NOT EXISTS rooms (
                                     roomId        BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
                                     roomNumber    VARCHAR(50)  NOT NULL UNIQUE,
    type          VARCHAR(20)  NOT NULL,
    pricePerNight DOUBLE       NOT NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'AVAILABLE',
    floor         INT
    );

-- Bảng customers
CREATE TABLE IF NOT EXISTS customers (
                                         customerId     VARCHAR(36)  NOT NULL PRIMARY KEY,
    fullName       VARCHAR(255) NOT NULL,
    phoneNumber    VARCHAR(20)  NOT NULL UNIQUE,
    email          VARCHAR(255) NOT NULL,
    identityNumber VARCHAR(20)  NOT NULL UNIQUE,
    createdAt      DATETIME
    );

-- Bảng bookings
CREATE TABLE IF NOT EXISTS bookings (
                                        bookingId     BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
                                        customerId    VARCHAR(36) NOT NULL,
    roomId        BIGINT      NOT NULL,
    checkedInDate DATE        NOT NULL,
    checkedOutDate DATE       NOT NULL,
    status        VARCHAR(20) NOT NULL,
    totalPrice    DOUBLE,
    createdAt     DATETIME,
    UNIQUE KEY uq_room_dates (roomId, checkedInDate, checkedOutDate),
    CONSTRAINT fk_booking_customer FOREIGN KEY (customerId) REFERENCES customers(customerId),
    CONSTRAINT fk_booking_room     FOREIGN KEY (roomId)     REFERENCES rooms(roomId)
    );

-- Bảng invalidatedTokes (blacklist JWT)
CREATE TABLE IF NOT EXISTS invalidatedTokes (
                                                tokenId    VARCHAR(255) NOT NULL PRIMARY KEY,
    expiryTime DATETIME
    );

-- Bảng audit_log
CREATE TABLE IF NOT EXISTS audit_log (
                                         performerId BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
                                         username    VARCHAR(255),
    action      VARCHAR(50),
    entityName  VARCHAR(100),
    entityId    VARCHAR(100),
    detail      TEXT,
    ipAddress   VARCHAR(50),
    createAt    DATETIME,
    auditStatus VARCHAR(20)
    );