package com.example.resort.dto.response;

import com.example.resort.enums.PaymentMethod;
import com.example.resort.enums.PaymentStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentCheckoutResponse {
    private String paymentGroupId;
    private List<Long> bookingIds;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private Double amount;
    private String paymentUrl;
    private String vnpayTransactionId;
    private LocalDateTime createdAt;
    private LocalDateTime paymentExpiredAt;
    private LocalDateTime paidAt;
    private List<PaymentResponse> payments;
}
