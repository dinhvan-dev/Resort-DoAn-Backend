package com.example.resort.service;

import com.example.resort.dto.request.booking.BookingCreateRequest;
import com.example.resort.dto.response.BookingResponse;
import com.example.resort.dto.response.PageResponse;
import com.example.resort.entity.AuditLog;
import com.example.resort.entity.Booking;
import com.example.resort.entity.customer.Customer;
import com.example.resort.entity.room.Room;
import com.example.resort.enums.booking.BookingStatus;
import com.example.resort.enums.rooms.RoomStatus;
import com.example.resort.exception.AppException;
import com.example.resort.exception.ErrorCode;
import com.example.resort.mapper.BookingMapper;
import com.example.resort.repository.BookingRepository;
import com.example.resort.repository.CustomerRepository;
import com.example.resort.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@RequiredArgsConstructor
@Service
public class BookingService {

    private final BookingMapper bookingMapper;
    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final CustomerRepository customerRepository;
    private final AuditLogService auditLogService;
    private final CleaningTaskService cleaningTaskService;

    @CacheEvict(value = {"bookings", "booking"}, allEntries = true)
    @Transactional
    public BookingResponse createBooking(BookingCreateRequest request) {

        try {

            validateDateRange(request.getCheckInDate(), request.getCheckOutDate());

            Room room = roomRepository.findByIdWithLock(request.getRoomId())
                    .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));
            validateRoomBookable(room);

            Customer customer = customerRepository.findActiveByCustomerId(request.getCustomerId())
                    .orElseThrow(() -> new AppException(ErrorCode.CUSTOMER_NOT_FOUND));
            validateCustomerAccess(customer);

            if (bookingRepository.isRoomUnavailable(
                    room.getRoomId(),
                    request.getCheckInDate(),
                    request.getCheckOutDate(), null
            ))
                throw new AppException(ErrorCode.BOOKING_ROOM_UNAVAILABLE);

            double totalPrice = room.getPricePerNight() * ChronoUnit.DAYS.between(request.getCheckInDate(), request.getCheckOutDate());



            Booking booking = Booking.builder()
                    .customer(customer)
                    .room(room)
                    .checkedInDate(request.getCheckInDate())
                    .checkedOutDate(request.getCheckOutDate())
                    .status(BookingStatus.PENDING)
                    .totalPrice(totalPrice)
                    .build();

            BookingResponse response = bookingMapper.toBookingResponse(bookingRepository.save(booking));

            // AuditLog success
            auditLogService.log(
                    "CREATE", "Booking",
                    String.valueOf(response.getBookingId()),
                    "Created booking for room" + room.getRoomNumber()
                            + "from" + request.getCheckInDate()
                            + "to" + request.getCheckOutDate(),
                    AuditLog.AuditStatus.SUCCESS
            );

            return response;
        }
        catch (AppException e)
        {
            // Audit Log Failed
            auditLogService.log(
                    "CREATE", "Booking", "N/A",
                    "Failed to create booking - " + e.getErrorCode().getMessage(),
                    AuditLog.AuditStatus.FAILED
            );
            throw e;
        }

    }

    private void validateDateRange(LocalDate checkIn, LocalDate checkOut)
    {
        if (checkIn.isAfter(checkOut) || checkIn.isEqual(checkOut))
        {
            throw new AppException(ErrorCode.BOOKING_INVALID_DATE);
        }
    }

    @Cacheable(value = "bookings", key = "#page + '-' + #size")
    @Transactional(readOnly = true)
    public PageResponse<BookingResponse> getAllBookings( int page, int size)
    {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Booking> bookingsPage = bookingRepository.findAllActive(pageable);

        List<BookingResponse> data = bookingsPage.getContent()
                .stream()
                .map(bookingMapper::toBookingResponse)
                .toList();

        return PageResponse.<BookingResponse> builder()
                .data(data)
                .currentPage(bookingsPage.getNumber())
                .pageSize(bookingsPage.getSize())
                .totalElements(bookingsPage.getTotalElements())
                .totalPages(bookingsPage.getTotalPages())
                .first(bookingsPage.isFirst())
                .last(bookingsPage.isLast())
                .build();

    }

    @Cacheable(value = "booking", key = "#bookingId")
    @Transactional(readOnly = true)
    public BookingResponse getBookingById(Long bookingId)
    {
        Booking booking = findBookingById(bookingId);
        validateCustomerAccess(booking.getCustomer());
        return bookingMapper.toBookingResponse(booking);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getMyBookings()
    {
        return bookingRepository.findActiveByUsername(currentUsername())
                .stream()
                .map(bookingMapper::toBookingResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getBookingsByCustomerId(String customerId)
    {
        if (!customerRepository.existsActiveByCustomerId(customerId))
        {
            throw new AppException(ErrorCode.CUSTOMER_NOT_FOUND);
        }
        return bookingRepository.findActiveByCustomerId(customerId)
                .stream()
                .map(bookingMapper::toBookingResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getBookingsByRoomId(Long roomId)
    {
        if (!roomRepository.existsById(roomId))
        {
            throw new AppException(ErrorCode.ROOM_NOT_FOUND);
        }
       return bookingRepository.findActiveByRoomId(roomId)
               .stream().map(bookingMapper::toBookingResponse)
               .toList();
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getBookingsByStatus(BookingStatus status)
    {
        return bookingRepository.findActiveByStatus(status)
                .stream()
                .map(bookingMapper::toBookingResponse)
                .toList();
    }

    @CacheEvict(value = {"bookings", "booking"}, allEntries = true)
    @Transactional
    public BookingResponse updateBookingDates(Long bookingId,
                                              LocalDate newCheckInDate,
                                              LocalDate newCheckOutDate)
    {
        try
        {
            Booking booking = findBookingById(bookingId);
            validateDateRange(newCheckInDate, newCheckOutDate);
            validateRoomBookable(booking.getRoom());

            if (booking.getStatus() != BookingStatus.PENDING && booking.getStatus() != BookingStatus.CONFIRMED)
                throw new AppException(ErrorCode.BOOKING_INVALID_STATUS_TRANSITION);

            if (bookingRepository.isRoomUnavailable(booking.getRoom().getRoomId(), newCheckInDate, newCheckOutDate, booking.getBookingId()))
                throw new AppException(ErrorCode.BOOKING_ROOM_UNAVAILABLE);

            double newTotalPrice = booking.getRoom().getPricePerNight() * ChronoUnit.DAYS.between(newCheckInDate, newCheckOutDate);

            booking.setCheckedInDate(newCheckInDate);
            booking.setCheckedOutDate(newCheckOutDate);
            booking.setTotalPrice(newTotalPrice);

            BookingResponse response = bookingMapper.toBookingResponse(bookingRepository.save(booking));

            auditLogService.log(
                    "UPDATE", "Booking",
                    String.valueOf(bookingId),
                    "Updated dates to - " + newCheckInDate + "-" + newCheckOutDate,
                    AuditLog.AuditStatus.SUCCESS
            );

            return response;
        }
        catch (AppException e)
        {
            auditLogService.log(
                    "UPDATE", "Booking",
                    String.valueOf(bookingId),
                    "Failed to update dates - " + e.getErrorCode().getMessage(),
                    AuditLog.AuditStatus.FAILED
            );
            throw e;
        }
    }

    @CacheEvict(value = {"bookings", "booking"}, allEntries = true)
    @Transactional
    public void cancelBooking(Long bookingId)
    {
        Booking booking = findBookingById(bookingId);
        if (booking.getStatus() == BookingStatus.CANCELLED)
        {
            throw new AppException(ErrorCode.BOOKING_ALREADY_CANCELLED);
        }
        if (booking.getStatus() != BookingStatus.PENDING &&
                booking.getStatus() != BookingStatus.CONFIRMED)
        {
            throw new AppException(ErrorCode.BOOKING_CANNOT_CANCEL);
        }

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);
    }

    @CacheEvict(value = {"bookings", "booking", "rooms", "room"}, allEntries = true)
    @Transactional
    public BookingResponse checkIn(Long bookingId)
    {
        Booking booking = findBookingById(bookingId);
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new AppException(ErrorCode.BOOKING_INVALID_STATUS_TRANSITION);
        }
        validateRoomBookable(booking.getRoom());

        booking.setStatus(BookingStatus.CHECKED_IN);
        booking.getRoom().setStatus(RoomStatus.OCCUPIED);
        roomRepository.save(booking.getRoom());

        return bookingMapper.toBookingResponse(bookingRepository.save(booking));
    }

    @CacheEvict(value = {"bookings", "booking", "rooms", "room"}, allEntries = true)
    @Transactional
    public BookingResponse checkOut(Long bookingId)
    {
        Booking booking = findBookingById(bookingId);
        if (booking.getStatus() != BookingStatus.CHECKED_IN) {
            throw new AppException(ErrorCode.BOOKING_INVALID_STATUS_TRANSITION);
        }

        booking.setStatus(BookingStatus.CHECKED_OUT);
        booking.getRoom().setStatus(RoomStatus.NEEDS_CLEANING);
        roomRepository.save(booking.getRoom());
        cleaningTaskService.createTaskForCheckout(booking.getRoom(), booking);

        return bookingMapper.toBookingResponse(bookingRepository.save(booking));
    }

    private Booking findBookingById(Long bookingId)
    {
        return bookingRepository.findActiveById(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));
    }

    private void validateRoomBookable(Room room)
    {
        if (room.getStatus() == RoomStatus.MAINTENANCE)
        {
            throw new AppException(ErrorCode.ROOM_UNDER_MAINTENANCE);
        }
        if (room.getStatus() != RoomStatus.AVAILABLE)
        {
            throw new AppException(ErrorCode.ROOM_NOT_AVAILABLE);
        }
    }

    private void validateCustomerAccess(Customer customer)
    {
        if (!hasRole("ROLE_USER")) {
            return;
        }
        if (customer.getUser() == null || !currentUsername().equals(customer.getUser().getUsername())) {
            throw new AppException(ErrorCode.CUSTOMER_ACCESS_DENIED);
        }
    }

    private String currentUsername()
    {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private boolean hasRole(String role)
    {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                .stream()
                .anyMatch(authority -> authority.getAuthority().equals(role));
    }
}
