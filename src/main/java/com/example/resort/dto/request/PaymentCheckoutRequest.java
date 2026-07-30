package com.example.resort.dto.request;

import com.example.resort.enums.PaymentMethod;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentCheckoutRequest {
    @NotEmpty(message = "Booking ids are required")
    private List<Long> bookingIds;

    @NotNull(message = "Payment Method is required")
    private PaymentMethod paymentMethod;
}
