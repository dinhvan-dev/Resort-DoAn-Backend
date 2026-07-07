package com.example.resort.dto.request;

import com.example.resort.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentRequest {
    @NotNull(message = "Booking Id is required")
    private Long bookingId;

    @NotNull(message = "Payment Method is required")
    private PaymentMethod paymentMethod; // VNPay hoặc CASH
}
