package com.example.resort.dto.response.report;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminSummaryResponse {
    private Double totalRevenue;
    private Double monthlyRevenue;
    private Long totalBookings;
    private Long pendingBookings;
    private Long confirmedBookings;
    private Long checkedInBookings;
    private Long totalRooms;
    private Long availableRooms;
    private Long occupiedRooms;
    private Long needsCleaningRooms;
    private Long totalCustomers;
    private Long totalUsers;
    private Long pendingPayments;
    private Long paidPayments;
    private Long failedPayments;
    private List<StatusCountResponse> bookingStatusCounts;
    private List<StatusCountResponse> roomStatusCounts;
    private List<StatusCountResponse> paymentStatusCounts;
    private List<RevenuePointResponse> revenueByDay;
    private List<TopRoomResponse> topRooms;
    private List<RecentBookingResponse> recentBookings;
}
