package com.example.resort.dto.response.staff;

import com.example.resort.dto.response.BookingResponse;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StaffSummaryResponse {
    private Long todayCheckInsCount;
    private Long todayCheckOutsCount;
    private Long cashPendingPaymentsCount;
    private Long cleaningPendingCount;
    private Long cleaningDoneCount;
    private Long availableRooms;
    private Long occupiedRooms;
    private Long needsCleaningRooms;
    private List<BookingResponse> todayCheckIns;
    private List<BookingResponse> todayCheckOuts;
    private List<StaffPaymentResponse> cashPendingPayments;
}
