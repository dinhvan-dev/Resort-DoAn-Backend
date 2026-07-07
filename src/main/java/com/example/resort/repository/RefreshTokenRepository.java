package com.example.resort.repository;

import com.example.resort.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {
    Optional<RefreshToken> findByToken(String token);

    // xóa tất cả refreshToken của 1 user ( khi logout devices )
    void deleteByUsername(String username);

    // dọn dẹp token hê hạn
    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.expiryTime < :now OR r.revoked = true")
    void deleteExpiredRevokedTokens(@Param("now")LocalDateTime now);

}
