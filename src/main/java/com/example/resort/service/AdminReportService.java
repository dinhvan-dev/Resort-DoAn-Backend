package com.example.resort.service;

import com.example.resort.dto.response.report.*;
import com.example.resort.entity.Booking;
import com.example.resort.enums.PaymentStatus;
import com.example.resort.enums.booking.BookingStatus;
import com.example.resort.enums.rooms.RoomStatus;
import com.example.resort.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminReportService {
    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final PaymentRepository paymentRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public AdminSummaryResponse getSummary()
    {
        Map<BookingStatus, Long> bookingCounts = bookingStatusCounts();
        Map<RoomStatus, Long> roomCounts = roomStatusCounts();
        Map<PaymentStatus, Long> paymentCounts = paymentStatusCounts();

        YearMonth currentMonth = YearMonth.now();
        LocalDateTime monthStart = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime nextMonthStart = currentMonth.plusMonths(1).atDay(1).atStartOfDay();

        return AdminSummaryResponse.builder()
                .totalRevenue(nullToZero(paymentRepository.sumPaidRevenue()))
                .monthlyRevenue(nullToZero(paymentRepository.sumPaidRevenueBetween(monthStart, nextMonthStart)))
                .totalBookings(bookingRepository.countActiveBookings())
                .pendingBookings(bookingCounts.getOrDefault(BookingStatus.PENDING, 0L))
                .confirmedBookings(bookingCounts.getOrDefault(BookingStatus.CONFIRMED, 0L))
                .checkedInBookings(bookingCounts.getOrDefault(BookingStatus.CHECKED_IN, 0L))
                .totalRooms(roomRepository.countActiveRooms())
                .availableRooms(roomCounts.getOrDefault(RoomStatus.AVAILABLE, 0L))
                .occupiedRooms(roomCounts.getOrDefault(RoomStatus.OCCUPIED, 0L))
                .needsCleaningRooms(roomCounts.getOrDefault(RoomStatus.NEEDS_CLEANING, 0L))
                .totalCustomers(customerRepository.countActiveCustomers())
                .totalUsers(userRepository.countActiveUsers())
                .pendingPayments(paymentCounts.getOrDefault(PaymentStatus.PENDING, 0L)
                        + paymentCounts.getOrDefault(PaymentStatus.PROCESSING, 0L))
                .paidPayments(paymentCounts.getOrDefault(PaymentStatus.PAID, 0L))
                .failedPayments(paymentCounts.getOrDefault(PaymentStatus.FAILED, 0L))
                .bookingStatusCounts(toStatusCounts(bookingCounts))
                .roomStatusCounts(toStatusCounts(roomCounts))
                .paymentStatusCounts(toStatusCounts(paymentCounts))
                .revenueByDay(revenueByDay())
                .topRooms(topRooms())
                .recentBookings(recentBookings())
                .build();
    }

    private Map<BookingStatus, Long> bookingStatusCounts()
    {
        Map<BookingStatus, Long> counts = new EnumMap<>(BookingStatus.class);
        bookingRepository.countActiveBookingsByStatus()
                .forEach(row -> counts.put((BookingStatus) row[0], (Long) row[1]));
        return counts;
    }

    private Map<RoomStatus, Long> roomStatusCounts()
    {
        Map<RoomStatus, Long> counts = new EnumMap<>(RoomStatus.class);
        roomRepository.countActiveRoomsByStatus()
                .forEach(row -> counts.put((RoomStatus) row[0], (Long) row[1]));
        return counts;
    }

    private Map<PaymentStatus, Long> paymentStatusCounts()
    {
        Map<PaymentStatus, Long> counts = new EnumMap<>(PaymentStatus.class);
        paymentRepository.countActivePaymentsByStatus()
                .forEach(row -> counts.put((PaymentStatus) row[0], (Long) row[1]));
        return counts;
    }

    private <E extends Enum<E>> List<StatusCountResponse> toStatusCounts(Map<E, Long> counts)
    {
        return counts.entrySet().stream()
                .map(entry -> StatusCountResponse.builder()
                        .label(entry.getKey().name())
                        .count(entry.getValue())
                        .build())
                .toList();
    }

    private List<RevenuePointResponse> revenueByDay()
    {
        LocalDateTime start = LocalDate.now().minusDays(13).atStartOfDay();
        return paymentRepository.sumPaidRevenueByDay(start).stream()
                .map(row -> RevenuePointResponse.builder()
                        .date(toLocalDate(row[0]))
                        .amount(((Number) row[1]).doubleValue())
                        .build())
                .toList();
    }

    private List<TopRoomResponse> topRooms()
    {
        return bookingRepository.findTopRooms(PageRequest.of(0, 5)).stream()
                .map(row -> TopRoomResponse.builder()
                        .roomNumber(String.valueOf(row[0]))
                        .bookingCount((Long) row[1])
                        .revenue(((Number) row[2]).doubleValue())
                        .build())
                .toList();
    }

    private List<RecentBookingResponse> recentBookings()
    {
        return bookingRepository.findRecentActiveBookings(PageRequest.of(0, 6)).stream()
                .map(this::toRecentBooking)
                .toList();
    }

    private RecentBookingResponse toRecentBooking(Booking booking)
    {
        return RecentBookingResponse.builder()
                .bookingId(booking.getBookingId())
                .guestName(booking.getCustomer().getFullName())
                .roomNumber(booking.getRoom().getRoomNumber())
                .status(booking.getStatus().name())
                .totalPrice(booking.getTotalPrice())
                .checkedInDate(booking.getCheckedInDate())
                .checkedOutDate(booking.getCheckedOutDate())
                .createdAt(booking.getCreatedAt())
                .build();
    }

    private LocalDate toLocalDate(Object value)
    {
        if (value instanceof LocalDate localDate)
        {
            return localDate;
        }

        if (value instanceof Date date)
        {
            return date.toLocalDate();
        }

        return LocalDate.parse(String.valueOf(value));
    }

    private Double nullToZero(Double value)
    {
        return value == null ? 0 : value;
    }
}
