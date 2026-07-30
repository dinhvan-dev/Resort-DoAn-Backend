package com.example.resort.dto.response;

import com.example.resort.enums.PaymentMethod;
import com.example.resort.enums.PaymentStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentResponse {
    private String paymentId;
    private Long bookingId;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private Double amount;
    private String paymentUrl;
    private String vnpayTransactionId;
    private LocalDateTime createdAt;
    private LocalDateTime paymentExpiredAt;
    private LocalDateTime paidAt;
}
