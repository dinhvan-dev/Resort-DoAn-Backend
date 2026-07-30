package com.example.resort.dto.response.staff;

import com.example.resort.enums.PaymentMethod;
import com.example.resort.enums.PaymentStatus;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StaffPaymentResponse {
    private String paymentId;
    private Long bookingId;
    private String guestName;
    private String roomNumber;
    private LocalDate checkedInDate;
    private LocalDate checkedOutDate;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private Double amount;
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
}
