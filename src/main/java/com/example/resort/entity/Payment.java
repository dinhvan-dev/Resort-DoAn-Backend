package com.example.resort.entity;

import com.example.resort.enums.PaymentMethod;
import com.example.resort.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class Payment extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "payment_id")
    private String paymentId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_group_id")
    private PaymentGroup paymentGroup;

    @Column(name = "payment_method", nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    @Column(name = "payment_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;

    @Column(name = "vnpay_transaction_id")
    private String vnpayTransactionId;

    @Column(name = "vnpay_order_info")
    private String vnpayOrderInfo;

    @Column(name = "amount", nullable = false)
    private Double amount;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "payment_expired_at")
    private LocalDateTime paymentExpiredAt;
}
