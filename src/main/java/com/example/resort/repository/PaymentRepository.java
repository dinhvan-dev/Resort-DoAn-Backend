package com.example.resort.repository;

import com.example.resort.entity.Payment;
import com.example.resort.enums.PaymentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, String> {

    Optional<Payment> findByBooking_BookingId(Long bookingId);

    // FIX: "Vnpay" không phải "VNPay" — phải khớp với field vnpayTransactionId
    Optional<Payment> findByVnpayTransactionId(String transactionId);

    List<Payment> findByPaymentStatus(PaymentStatus status);

    @Query("SELECT p FROM Payment p WHERE p.paymentId = :paymentId AND p.isActive = true")
    Optional<Payment> findActiveById(@Param("paymentId") String paymentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.paymentId = :paymentId AND p.isActive = true")
    Optional<Payment> findActiveByIdForUpdate(@Param("paymentId") String paymentId);

    @Query("SELECT p FROM Payment p WHERE p.booking.bookingId = :bookingId AND p.isActive = true")
    Optional<Payment> findActiveByBookingId(@Param("bookingId") Long bookingId);

    @Query("SELECT p FROM Payment p WHERE p.booking.bookingId IN :bookingIds AND p.isActive = true")
    List<Payment> findActiveByBookingIds(@Param("bookingIds") List<Long> bookingIds);

    @Query("SELECT p FROM Payment p WHERE p.paymentGroup.paymentGroupId = :paymentGroupId AND p.isActive = true")
    List<Payment> findActiveByPaymentGroupId(@Param("paymentGroupId") String paymentGroupId);

    @Query("SELECT p FROM Payment p WHERE p.vnpayTransactionId = :transactionId AND p.isActive = true")
    Optional<Payment> findActiveByVnpayTransactionId(@Param("transactionId") String transactionId);

    @Query("SELECT p FROM Payment p WHERE p.paymentStatus = :status AND p.isActive = true")
    List<Payment> findActiveByPaymentStatus(@Param("status") PaymentStatus status);

    @Query("SELECT p FROM Payment p WHERE p.isActive = true ORDER BY p.createdAt DESC")
    List<Payment> findAllActiveOrderByCreatedAtDesc();

    @Query("SELECT p.paymentStatus, COUNT(p) FROM Payment p WHERE p.isActive = true GROUP BY p.paymentStatus")
    List<Object[]> countActivePaymentsByStatus();

    @Query("""
            SELECT COALESCE(SUM(p.amount), 0)
            FROM Payment p
            WHERE p.isActive = true
            AND p.paymentStatus = com.example.resort.enums.PaymentStatus.PAID
            """)
    Double sumPaidRevenue();

    @Query("""
            SELECT COALESCE(SUM(p.amount), 0)
            FROM Payment p
            WHERE p.isActive = true
            AND p.paymentStatus = com.example.resort.enums.PaymentStatus.PAID
            AND p.paidAt >= :start
            AND p.paidAt < :end
            """)
    Double sumPaidRevenueBetween(@Param("start") java.time.LocalDateTime start, @Param("end") java.time.LocalDateTime end);

    @Query("""
            SELECT FUNCTION('DATE', p.paidAt), COALESCE(SUM(p.amount), 0)
            FROM Payment p
            WHERE p.isActive = true
            AND p.paymentStatus = com.example.resort.enums.PaymentStatus.PAID
            AND p.paidAt >= :start
            GROUP BY FUNCTION('DATE', p.paidAt)
            ORDER BY FUNCTION('DATE', p.paidAt)
            """)
    List<Object[]> sumPaidRevenueByDay(@Param("start") java.time.LocalDateTime start);

    @Query("""
            SELECT p FROM Payment p
            WHERE p.isActive = true
            AND p.paymentMethod = com.example.resort.enums.PaymentMethod.CASH
            AND p.paymentStatus = com.example.resort.enums.PaymentStatus.PENDING
            ORDER BY p.createdAt DESC
            """)
    List<Payment> findPendingCashPayments();
}
