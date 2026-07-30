package com.example.resort.repository;

import com.example.resort.entity.room.Room;
import com.example.resort.enums.rooms.RoomType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;
import java.util.List;

public interface RoomRepository extends JpaRepository<Room, Long> {
    boolean existsByRoomNumber(String roomNumber);

    Optional<Room> findByRoomNumber(String roomNumber); // Tìm kiếm phòng theo số phòng

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Room r WHERE r.roomId = :roomId")
    Optional<Room> findByIdWithLock(@Param("roomId") Long roomId);

    void deleteByRoomNumber(String roomNumber);

    @Query("SELECT r.status, COUNT(r) FROM Room r GROUP BY r.status")
    List<Object[]> countActiveRoomsByStatus();

    @Query("SELECT COUNT(r) FROM Room r")
    long countActiveRooms();

    @Query("""
            SELECT r FROM Room r
            WHERE r.type = :type
            AND r.status <> com.example.resort.enums.rooms.RoomStatus.MAINTENANCE
            AND r.roomId NOT IN (
                SELECT b.room.roomId FROM Booking b
                WHERE b.isActive = true
                AND b.room IS NOT NULL
                AND b.status NOT IN (
                    com.example.resort.enums.booking.BookingStatus.CANCELLED,
                    com.example.resort.enums.booking.BookingStatus.CHECKED_OUT
                )
                AND b.checkedInDate < :checkOutDate
                AND b.checkedOutDate > :checkInDate
            )
            ORDER BY r.pricePerNight ASC, r.roomNumber ASC
            """)
    List<Room> findAvailableRoomsByType(
            @Param("type") RoomType type,
            @Param("checkInDate") LocalDate checkInDate,
            @Param("checkOutDate") LocalDate checkOutDate
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT r FROM Room r
            WHERE r.type = :type
            AND r.status <> com.example.resort.enums.rooms.RoomStatus.MAINTENANCE
            AND r.roomId NOT IN (
                SELECT b.room.roomId FROM Booking b
                WHERE b.isActive = true
                AND b.room IS NOT NULL
                AND b.status NOT IN (
                    com.example.resort.enums.booking.BookingStatus.CANCELLED,
                    com.example.resort.enums.booking.BookingStatus.CHECKED_OUT
                )
                AND b.checkedInDate < :checkOutDate
                AND b.checkedOutDate > :checkInDate
            )
            ORDER BY r.pricePerNight ASC, r.roomNumber ASC
            """)
    List<Room> findAvailableRoomsByTypeForUpdate(
            @Param("type") RoomType type,
            @Param("checkInDate") LocalDate checkInDate,
            @Param("checkOutDate") LocalDate checkOutDate
    );

    @Query("""
            SELECT r FROM Room r
            WHERE r.type = :type
            AND r.status <> com.example.resort.enums.rooms.RoomStatus.MAINTENANCE
            ORDER BY r.pricePerNight ASC, r.roomNumber ASC
            """)
    List<Room> findByTypeOrderByPrice(@Param("type") RoomType type);
}
