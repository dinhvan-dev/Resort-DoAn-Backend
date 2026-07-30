package com.example.resort.entity;

import com.example.resort.enums.PaymentMethod;
import com.example.resort.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "payment_groups")
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentGroup extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "payment_group_id")
    private String paymentGroupId;

    @Column(name = "payment_method", nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    @Column(name = "payment_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;

    @Column(name = "amount", nullable = false)
    private Double amount;

    @Column(name = "vnpay_transaction_id")
    private String vnpayTransactionId;

    @Column(name = "vnpay_order_info")
    private String vnpayOrderInfo;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "payment_expired_at")
    private LocalDateTime paymentExpiredAt;

    @Builder.Default
    @OneToMany(mappedBy = "paymentGroup")
    private List<Payment> payments = new ArrayList<>();
}
