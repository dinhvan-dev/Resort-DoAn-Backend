package com.example.resort.repository;

import com.example.resort.entity.Payment;
import com.example.resort.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
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

    @Query("SELECT p FROM Payment p WHERE p.booking.bookingId = :bookingId AND p.isActive = true")
    Optional<Payment> findActiveByBookingId(@Param("bookingId") Long bookingId);

    @Query("SELECT p FROM Payment p WHERE p.vnpayTransactionId = :transactionId AND p.isActive = true")
    Optional<Payment> findActiveByVnpayTransactionId(@Param("transactionId") String transactionId);

    @Query("SELECT p FROM Payment p WHERE p.paymentStatus = :status AND p.isActive = true")
    List<Payment> findActiveByPaymentStatus(@Param("status") PaymentStatus status);
}
