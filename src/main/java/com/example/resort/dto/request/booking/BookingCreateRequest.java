package com.example.resort.dto.request.booking;

import com.example.resort.enums.rooms.RoomType;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BookingCreateRequest {

    @NotNull(message = "Customer ID is required")
    private String customerId;

    private Long roomId;

    @NotNull(message = "Room type is required")
    private RoomType roomType;

    @NotNull(message = "Room quantity is required")
    @Min(value = 1, message = "Room quantity must be greater than 0")
    private Integer quantity;

    @NotNull(message = "Number of guests is required")
    @Min(value = 1, message = "Number of guests must be greater than 0")
    private Integer numberOfGuests;

    @NotNull(message = "Check-in date is required")
    @FutureOrPresent(message = "Check-in date cannot be in the past")
    private LocalDate checkInDate;

    private LocalTime checkInTime;

    @NotNull(message = "Check-out date is required")
    @Future(message = "Check-out date must be in the future")
    private LocalDate checkOutDate;

    private LocalTime checkOutTime;

}
