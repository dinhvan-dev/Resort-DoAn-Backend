ALTER TABLE bookings
    MODIFY roomId BIGINT NULL,
    ADD COLUMN roomType VARCHAR(20) NULL,
    ADD COLUMN quantity INT NOT NULL DEFAULT 1,
    ADD COLUMN numberOfGuests INT NOT NULL DEFAULT 1,
    ADD COLUMN pricePerNight DOUBLE NULL,
    ADD COLUMN numberOfNights INT NULL,
    ADD COLUMN subtotal DOUBLE NULL,
    ADD COLUMN taxAmount DOUBLE NOT NULL DEFAULT 0,
    ADD COLUMN serviceFee DOUBLE NOT NULL DEFAULT 0,
    ADD COLUMN discountAmount DOUBLE NOT NULL DEFAULT 0,
    ADD COLUMN totalAmount DOUBLE NULL;

UPDATE bookings b
LEFT JOIN rooms r ON r.roomId = b.roomId
SET
    b.roomType = COALESCE(b.roomType, r.type),
    b.quantity = COALESCE(b.quantity, 1),
    b.numberOfGuests = COALESCE(b.numberOfGuests, 1),
    b.pricePerNight = COALESCE(b.pricePerNight, r.pricePerNight, b.totalPrice),
    b.numberOfNights = COALESCE(b.numberOfNights, GREATEST(DATEDIFF(b.checkedOutDate, b.checkedInDate), 1)),
    b.subtotal = COALESCE(b.subtotal, COALESCE(r.pricePerNight, b.totalPrice, 0) * GREATEST(DATEDIFF(b.checkedOutDate, b.checkedInDate), 1)),
    b.totalAmount = COALESCE(b.totalAmount, b.totalPrice);
