CREATE TABLE IF NOT EXISTS cleaning_tasks (
    taskId BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    roomId BIGINT NOT NULL,
    bookingId BIGINT NULL,
    assigned_to VARCHAR(255) NULL,
    status VARCHAR(30) NOT NULL,
    started_at DATETIME NULL,
    completed_at DATETIME NULL,
    verified_at DATETIME NULL,
    created_at DATETIME NULL,
    updated_at DATETIME NULL,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    CONSTRAINT fk_cleaning_task_room FOREIGN KEY (roomId) REFERENCES rooms(roomId),
    CONSTRAINT fk_cleaning_task_booking FOREIGN KEY (bookingId) REFERENCES bookings(bookingId)
);
