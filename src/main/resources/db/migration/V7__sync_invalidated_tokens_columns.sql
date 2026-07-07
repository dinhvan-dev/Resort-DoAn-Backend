ALTER TABLE invalidatedTokes
    CHANGE COLUMN tokenId token_id VARCHAR(255) NOT NULL,
    CHANGE COLUMN expiryTime expiry_time DATETIME NULL;
