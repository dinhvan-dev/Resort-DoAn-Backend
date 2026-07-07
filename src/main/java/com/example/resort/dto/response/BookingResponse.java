package com.example.resort.dto.response;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BookingResponse {
    private Long bookingId;
    private String customerId;
    private String fullName;
    private Long roomId;
    private String roomNumber;
    private LocalDate checkedInDate;
    private LocalDate checkedOutDate;
    private String status;
    private Double totalPrice;
    private LocalDateTime createdAt;
}
