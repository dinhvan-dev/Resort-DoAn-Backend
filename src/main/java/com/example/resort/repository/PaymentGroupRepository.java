package com.example.resort.repository;

import com.example.resort.entity.PaymentGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PaymentGroupRepository extends JpaRepository<PaymentGroup, String> {
    @Query("SELECT pg FROM PaymentGroup pg WHERE pg.paymentGroupId = :paymentGroupId AND pg.isActive = true")
    Optional<PaymentGroup> findActiveById(@Param("paymentGroupId") String paymentGroupId);

    @Query("SELECT pg FROM PaymentGroup pg WHERE pg.vnpayTransactionId = :transactionId AND pg.isActive = true")
    Optional<PaymentGroup> findActiveByVnpayTransactionId(@Param("transactionId") String transactionId);
}
