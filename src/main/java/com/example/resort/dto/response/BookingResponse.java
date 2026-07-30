package com.example.resort.dto.response;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

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
    private String roomType;
    private Integer quantity;
    private Integer numberOfGuests;
    private LocalDate checkedInDate;
    private LocalTime checkedInTime;
    private LocalDate checkedOutDate;
    private LocalTime checkedOutTime;
    private String status;
    private String paymentStatus;
    private Double pricePerNight;
    private Integer numberOfNights;
    private Double subtotal;
    private Double taxAmount;
    private Double serviceFee;
    private Double discountAmount;
    private Double totalAmount;
    private Double totalPrice;
    private LocalDateTime paymentExpiredAt;
    private LocalDateTime createdAt;
}
