package com.example.resort.service;

import com.example.resort.dto.response.BookingResponse;
import com.example.resort.dto.response.staff.StaffPaymentResponse;
import com.example.resort.dto.response.staff.StaffSummaryResponse;
import com.example.resort.entity.Booking;
import com.example.resort.entity.Payment;
import com.example.resort.enums.cleaning.CleaningTaskStatus;
import com.example.resort.enums.rooms.RoomStatus;
import com.example.resort.mapper.BookingMapper;
import com.example.resort.repository.BookingRepository;
import com.example.resort.repository.CleaningTaskRepository;
import com.example.resort.repository.PaymentRepository;
import com.example.resort.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StaffReportService {
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final RoomRepository roomRepository;
    private final CleaningTaskRepository cleaningTaskRepository;
    private final BookingMapper bookingMapper;

    @Transactional(readOnly = true)
    public StaffSummaryResponse getSummary()
    {
        LocalDate today = LocalDate.now();
        List<BookingResponse> checkIns = bookingRepository.findTodayCheckIns(today)
                .stream()
                .map(bookingMapper::toBookingResponse)
                .toList();
        List<BookingResponse> checkOuts = bookingRepository.findTodayCheckOuts(today)
                .stream()
                .map(bookingMapper::toBookingResponse)
                .toList();
        List<StaffPaymentResponse> cashPendingPayments = getCashPendingPayments();
        Map<RoomStatus, Long> roomCounts = roomStatusCounts();

        return StaffSummaryResponse.builder()
                .todayCheckInsCount((long) checkIns.size())
                .todayCheckOutsCount((long) checkOuts.size())
                .cashPendingPaymentsCount((long) cashPendingPayments.size())
                .cleaningPendingCount(cleaningTaskRepository.countActiveByStatus(CleaningTaskStatus.PENDING))
                .cleaningDoneCount(cleaningTaskRepository.countActiveByStatus(CleaningTaskStatus.DONE))
                .availableRooms(roomCounts.getOrDefault(RoomStatus.AVAILABLE, 0L))
                .occupiedRooms(roomCounts.getOrDefault(RoomStatus.OCCUPIED, 0L))
                .needsCleaningRooms(roomCounts.getOrDefault(RoomStatus.NEEDS_CLEANING, 0L))
                .todayCheckIns(checkIns)
                .todayCheckOuts(checkOuts)
                .cashPendingPayments(cashPendingPayments)
                .build();
    }

    @Transactional(readOnly = true)
    public List<StaffPaymentResponse> getCashPendingPayments()
    {
        return paymentRepository.findPendingCashPayments()
                .stream()
                .map(this::toStaffPaymentResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StaffPaymentResponse> getStaffPayments()
    {
        return paymentRepository.findAllActiveOrderByCreatedAtDesc()
                .stream()
                .map(this::toStaffPaymentResponse)
                .toList();
    }

    private Map<RoomStatus, Long> roomStatusCounts()
    {
        Map<RoomStatus, Long> counts = new EnumMap<>(RoomStatus.class);
        roomRepository.countActiveRoomsByStatus()
                .forEach(row -> counts.put((RoomStatus) row[0], (Long) row[1]));
        return counts;
    }

    private StaffPaymentResponse toStaffPaymentResponse(Payment payment)
    {
        Booking booking = payment.getBooking();
        return StaffPaymentResponse.builder()
                .paymentId(payment.getPaymentId())
                .bookingId(booking.getBookingId())
                .guestName(booking.getCustomer().getFullName())
                .roomNumber(booking.getRoom() == null ? null : booking.getRoom().getRoomNumber())
                .checkedInDate(booking.getCheckedInDate())
                .checkedOutDate(booking.getCheckedOutDate())
                .paymentMethod(payment.getPaymentMethod())
                .paymentStatus(payment.getPaymentStatus())
                .amount(payment.getAmount())
                .createdAt(payment.getCreatedAt())
                .paidAt(payment.getPaidAt())
                .build();
    }
}
