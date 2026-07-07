-- V5: Đồng bộ lại column names cho audit_log và tạo refresh_tokens nếu chưa có

-- Xóa bảng audit_log cũ (do V1 tạo với column names sai)
-- và tạo lại với đúng column names khớp entity AuditLog.java
DROP TABLE IF EXISTS audit_log;

CREATE TABLE audit_log (
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

-- Tạo bảng refresh_tokens nếu chưa có (V2 cũ đã tạo sai)
CREATE TABLE IF NOT EXISTS refresh_tokens (
                                              id           VARCHAR(36)  NOT NULL PRIMARY KEY,
    token        VARCHAR(255) NOT NULL UNIQUE,
    username     VARCHAR(255) NOT NULL,
    expiry_time  DATETIME     NOT NULL,
    revoked      TINYINT(1)   NOT NULL DEFAULT 0,
    created_at   DATETIME     NOT NULL
    );