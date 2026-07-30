package com.example.resort.dto.response.report;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RecentBookingResponse {
    private Long bookingId;
    private String guestName;
    private String roomNumber;
    private String status;
    private Double totalPrice;
    private LocalDate checkedInDate;
    private LocalDate checkedOutDate;
    private LocalDateTime createdAt;
}
