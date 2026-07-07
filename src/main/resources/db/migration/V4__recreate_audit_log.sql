CREATE TABLE IF NOT EXISTS audit_log (
                                         performer_id BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
                                         username     VARCHAR(255),
    action       VARCHAR(50),
    entity_name  VARCHAR(100),
    entity_id    VARCHAR(100),
    detail       TEXT,
    ip_address   VARCHAR(50),
    created_at   DATETIME,
    audit_status VARCHAR(20)
    );