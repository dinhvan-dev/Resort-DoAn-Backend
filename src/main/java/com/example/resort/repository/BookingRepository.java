package com.example.resort.repository;

import com.example.resort.entity.Booking;
import com.example.resort.enums.booking.BookingStatus;
import com.example.resort.enums.rooms.RoomType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Query("""
            SELECT COUNT(b) > 0 FROM Booking b
            WHERE b.room.roomId = :roomId
            AND b.isActive = true
            AND b.status NOT IN (
                com.example.resort.enums.booking.BookingStatus.CANCELLED,
                com.example.resort.enums.booking.BookingStatus.CHECKED_OUT
            )
            AND b.checkedInDate < :checkedOutDate
            AND b.checkedOutDate > :checkInDate
            AND (:excludeBookingId IS NULL OR b.bookingId <> :excludeBookingId)
            """)
    boolean isRoomUnavailable(
            @Param("roomId") Long roomId,
            @Param("checkInDate") LocalDate checkInDate,
            @Param("checkedOutDate") LocalDate checkedOutDate,
            @Param("excludeBookingId") Long excludeBookingId
    );

    @Query("""
            SELECT COALESCE(SUM(COALESCE(b.quantity, 1)), 0) FROM Booking b
            WHERE b.isActive = true
            AND b.roomType = :roomType
            AND b.status NOT IN (
                com.example.resort.enums.booking.BookingStatus.CANCELLED,
                com.example.resort.enums.booking.BookingStatus.CHECKED_OUT
            )
            AND b.checkedInDate < :checkedOutDate
            AND b.checkedOutDate > :checkInDate
            AND (:excludeBookingId IS NULL OR b.bookingId <> :excludeBookingId)
            """)
    Long sumReservedQuantityByType(
            @Param("roomType") RoomType roomType,
            @Param("checkInDate") LocalDate checkInDate,
            @Param("checkedOutDate") LocalDate checkedOutDate,
            @Param("excludeBookingId") Long excludeBookingId
    );

    List<Booking> findByCustomer_CustomerId(String customerId);

    List<Booking> findByRoom_RoomId(Long roomId);

    List<Booking> findByStatus(BookingStatus status);

    @Query("SELECT b FROM Booking b WHERE b.isActive = true")
    Page<Booking> findAllActive(Pageable pageable);

    @Query("SELECT b FROM Booking b WHERE b.bookingId = :bookingId AND b.isActive = true")
    Optional<Booking> findActiveById(@Param("bookingId") Long bookingId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Booking b WHERE b.bookingId = :bookingId AND b.isActive = true")
    Optional<Booking> findActiveByIdForUpdate(@Param("bookingId") Long bookingId);

    @Query("SELECT b FROM Booking b WHERE b.customer.customerId = :customerId AND b.isActive = true")
    List<Booking> findActiveByCustomerId(@Param("customerId") String customerId);

    @Query("SELECT b FROM Booking b WHERE b.customer.user.username = :username AND b.isActive = true ORDER BY b.createdAt DESC")
    List<Booking> findActiveByUsername(@Param("username") String username);

    @Query("SELECT b FROM Booking b WHERE b.room.roomId = :roomId AND b.isActive = true")
    List<Booking> findActiveByRoomId(@Param("roomId") Long roomId);

    @Query("SELECT b FROM Booking b WHERE b.status = :status AND b.isActive = true")
    List<Booking> findActiveByStatus(@Param("status") BookingStatus status);

    @Query("SELECT b.status, COUNT(b) FROM Booking b WHERE b.isActive = true GROUP BY b.status")
    List<Object[]> countActiveBookingsByStatus();

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.isActive = true")
    long countActiveBookings();

    @Query("""
            SELECT b.room.roomNumber, COUNT(b), COALESCE(SUM(b.totalPrice), 0)
            FROM Booking b
            WHERE b.isActive = true
            GROUP BY b.room.roomNumber
            ORDER BY COUNT(b) DESC
            """)
    List<Object[]> findTopRooms(Pageable pageable);

    @Query("SELECT b FROM Booking b WHERE b.isActive = true ORDER BY b.createdAt DESC")
    Slice<Booking> findRecentActiveBookings(Pageable pageable);

    @Query("""
            SELECT b FROM Booking b
            WHERE b.isActive = true
            AND b.checkedInDate = :date
            AND b.status = com.example.resort.enums.booking.BookingStatus.CONFIRMED
            ORDER BY b.checkedInDate ASC, b.checkedInTime ASC, b.createdAt ASC
            """)
    List<Booking> findTodayCheckIns(@Param("date") LocalDate date);

    @Query("""
            SELECT b FROM Booking b
            WHERE b.isActive = true
            AND b.checkedOutDate = :date
            AND b.status = com.example.resort.enums.booking.BookingStatus.CHECKED_IN
            ORDER BY b.checkedOutDate ASC, b.checkedOutTime ASC, b.createdAt ASC
            """)
    List<Booking> findTodayCheckOuts(@Param("date") LocalDate date);
}
