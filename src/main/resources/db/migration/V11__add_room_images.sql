CREATE TABLE room_images (
    roomId BIGINT NOT NULL,
    sortOrder INT NOT NULL,
    imageUrl VARCHAR(1024) NOT NULL,
    PRIMARY KEY (roomId, sortOrder),
    CONSTRAINT fk_room_images_room
        FOREIGN KEY (roomId)
        REFERENCES rooms(roomId)
        ON DELETE CASCADE
);

INSERT INTO room_images (roomId, sortOrder, imageUrl)
SELECT roomId, 0, imageUrl
FROM rooms
WHERE imageUrl IS NOT NULL AND imageUrl <> '';
