package com.example.resort.repository;

import com.example.resort.entity.Booking;
import com.example.resort.enums.booking.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    List<Booking> findByCustomer_CustomerId(String customerId);

    List<Booking> findByRoom_RoomId(Long roomId);

    List<Booking> findByStatus(BookingStatus status);

    @Query("SELECT b FROM Booking b WHERE b.isActive = true")
    Page<Booking> findAllActive(Pageable pageable);

    @Query("SELECT b FROM Booking b WHERE b.bookingId = :bookingId AND b.isActive = true")
    Optional<Booking> findActiveById(@Param("bookingId") Long bookingId);

    @Query("SELECT b FROM Booking b WHERE b.customer.customerId = :customerId AND b.isActive = true")
    List<Booking> findActiveByCustomerId(@Param("customerId") String customerId);

    @Query("SELECT b FROM Booking b WHERE b.customer.user.username = :username AND b.isActive = true ORDER BY b.createdAt DESC")
    List<Booking> findActiveByUsername(@Param("username") String username);

    @Query("SELECT b FROM Booking b WHERE b.room.roomId = :roomId AND b.isActive = true")
    List<Booking> findActiveByRoomId(@Param("roomId") Long roomId);

    @Query("SELECT b FROM Booking b WHERE b.status = :status AND b.isActive = true")
    List<Booking> findActiveByStatus(@Param("status") BookingStatus status);
}
