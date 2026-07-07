CREATE TABLE IF NOT EXISTS refresh_tokens (
                                              id            VARCHAR(36)  NOT NULL PRIMARY KEY,
    token         VARCHAR(255) NOT NULL UNIQUE,
    username      VARCHAR(255) NOT NULL,
    expiry_time   DATETIME     NOT NULL,
    revoked       TINYINT(1)   NOT NULL DEFAULT 0,
    created_at    DATETIME     NOT NULL
    );