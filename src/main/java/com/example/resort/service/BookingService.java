package com.example.resort.service;

import com.example.resort.aop.logging.Auditable;
import com.example.resort.aop.event.PublishDomainEvent;
import com.example.resort.dto.request.booking.BookingCreateRequest;
import com.example.resort.dto.response.BookingResponse;
import com.example.resort.dto.response.PageResponse;
import com.example.resort.entity.Booking;
import com.example.resort.entity.customer.Customer;
import com.example.resort.entity.room.Room;
import com.example.resort.enums.booking.BookingStatus;
import com.example.resort.enums.rooms.RoomStatus;
import com.example.resort.enums.rooms.RoomType;
import com.example.resort.exception.AppException;
import com.example.resort.exception.ErrorCode;
import com.example.resort.mapper.BookingMapper;
import com.example.resort.repository.BookingRepository;
import com.example.resort.repository.CustomerRepository;
import com.example.resort.repository.PaymentRepository;
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
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@RequiredArgsConstructor
@Service
public class BookingService {
    private static final LocalTime DEFAULT_CHECK_IN_TIME = LocalTime.of(14, 0);
    private static final LocalTime DEFAULT_CHECK_OUT_TIME = LocalTime.of(12, 0);

    private final BookingMapper bookingMapper;
    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final PaymentRepository paymentRepository;
    private final CustomerRepository customerRepository;
    private final CleaningTaskService cleaningTaskService;

    @CacheEvict(value = {"bookings", "booking", "rooms", "room"}, allEntries = true)
    @Transactional
    @Auditable(
            action = "CREATE",
            entity = "Booking",
            entityId = "#result.bookingId",
            detail = "'Created booking ' + #result.bookingId + ' for ' + #result.roomType"
    )
    @PublishDomainEvent(
            type = "BOOKING_CREATED",
            aggregate = "Booking",
            aggregateId = "#result.bookingId"
    )
    public BookingResponse createBooking(BookingCreateRequest request) {
        validateDateRange(request.getCheckInDate(), request.getCheckOutDate());

        int quantity = request.getQuantity() == null ? 1 : request.getQuantity();
        int numberOfGuests = request.getNumberOfGuests() == null ? 1 : request.getNumberOfGuests();
        LocalTime checkInTime = request.getCheckInTime() == null ? DEFAULT_CHECK_IN_TIME : request.getCheckInTime();
        LocalTime checkOutTime = request.getCheckOutTime() == null ? DEFAULT_CHECK_OUT_TIME : request.getCheckOutTime();
        RoomType roomType = request.getRoomType();
        validateCapacity(roomType, numberOfGuests, quantity);

        Customer customer = customerRepository.findActiveByCustomerId(request.getCustomerId())
                .orElseThrow(() -> new AppException(ErrorCode.CUSTOMER_NOT_FOUND));
        validateCustomerAccess(customer);

        Room room = findRoomForBooking(request, roomType, quantity);
        int nights = Math.toIntExact(ChronoUnit.DAYS.between(request.getCheckInDate(), request.getCheckOutDate()));
        double pricePerNight = room.getPricePerNight();
        double subtotal = pricePerNight * nights * quantity;
        double taxAmount = Math.round(subtotal * 0.08);
        double serviceFee = Math.round(subtotal * 0.05);
        double discountAmount = 0D;
        double totalAmount = subtotal + taxAmount + serviceFee - discountAmount;

        Booking booking = Booking.builder()
                .customer(customer)
                .room(room)
                .roomType(roomType)
                .checkedInDate(request.getCheckInDate())
                .checkedInTime(checkInTime)
                .checkedOutDate(request.getCheckOutDate())
                .checkedOutTime(checkOutTime)
                .status(BookingStatus.PENDING)
                .quantity(quantity)
                .numberOfGuests(numberOfGuests)
                .pricePerNight(pricePerNight)
                .numberOfNights(nights)
                .subtotal(subtotal)
                .taxAmount(taxAmount)
                .serviceFee(serviceFee)
                .discountAmount(discountAmount)
                .totalAmount(totalAmount)
                .totalPrice(totalAmount)
                .build();

        return toBookingResponse(bookingRepository.save(booking));
    }

    private void validateDateRange(LocalDate checkIn, LocalDate checkOut) {
        if (checkIn == null || checkOut == null || !checkOut.isAfter(checkIn)) {
            throw new AppException(ErrorCode.BOOKING_INVALID_DATE);
        }

        if (checkIn.isBefore(LocalDate.now())) {
            throw new AppException(ErrorCode.BOOKING_INVALID_DATE);
        }
    }

    @Cacheable(value = "bookings", key = "#page + '-' + #size")
    @Transactional(readOnly = true)
    public PageResponse<BookingResponse> getAllBookings(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Booking> bookingsPage = bookingRepository.findAllActive(pageable);
        List<BookingResponse> data = bookingsPage.getContent()
                .stream()
                .map(this::toBookingResponse)
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
    public BookingResponse getBookingById(Long bookingId) {
        Booking booking = findBookingById(bookingId);
        validateCustomerAccess(booking.getCustomer());
        return toBookingResponse(booking);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getMyBookings() {
        return bookingRepository.findActiveByUsername(currentUsername())
                .stream()
                .map(this::toBookingResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getBookingsByCustomerId(String customerId) {
        if (!customerRepository.existsActiveByCustomerId(customerId)) {
            throw new AppException(ErrorCode.CUSTOMER_NOT_FOUND);
        }
        return bookingRepository.findActiveByCustomerId(customerId)
                .stream()
                .map(this::toBookingResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getBookingsByRoomId(Long roomId) {
        if (!roomRepository.existsById(roomId)) {
            throw new AppException(ErrorCode.ROOM_NOT_FOUND);
        }
       return bookingRepository.findActiveByRoomId(roomId)
               .stream().map(this::toBookingResponse)
               .toList();
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getBookingsByStatus(BookingStatus status) {
        return bookingRepository.findActiveByStatus(status)
                .stream()
                .map(this::toBookingResponse)
                .toList();
    }

    @CacheEvict(value = {"bookings", "booking"}, allEntries = true)
    @Transactional
    @Auditable(
            action = "UPDATE_DATES",
            entity = "Booking",
            entityId = "#result.bookingId",
            detail = "'Updated booking ' + #result.bookingId + ' dates to ' + #result.checkedInDate + ' - ' + #result.checkedOutDate"
    )
    @PublishDomainEvent(
            type = "BOOKING_DATES_UPDATED",
            aggregate = "Booking",
            aggregateId = "#result.bookingId"
    )
    public BookingResponse updateBookingDates(Long bookingId, LocalDate newCheckInDate, LocalDate newCheckOutDate) {
        Booking booking = findBookingById(bookingId);
        validateDateRange(newCheckInDate, newCheckOutDate);
        if (booking.getRoom() != null) {
            validateRoomBookable(booking.getRoom());
        }

        if (booking.getStatus() != BookingStatus.PENDING && booking.getStatus() != BookingStatus.CONFIRMED)
            throw new AppException(ErrorCode.BOOKING_INVALID_STATUS_TRANSITION);

        if (booking.getRoom() != null
                && bookingRepository.isRoomUnavailable(booking.getRoom().getRoomId(), newCheckInDate, newCheckOutDate, booking.getBookingId()))
            throw new AppException(ErrorCode.BOOKING_ROOM_UNAVAILABLE);

        ensureTypeQuantityAvailable(
                booking.getRoomType(),
                newCheckInDate,
                newCheckOutDate,
                booking.getQuantity(),
                booking.getBookingId()
        );

        int nights = Math.toIntExact(ChronoUnit.DAYS.between(newCheckInDate, newCheckOutDate));
        double newSubtotal = booking.getPricePerNight() * nights * booking.getQuantity();
        double taxAmount = Math.round(newSubtotal * 0.08);
        double serviceFee = Math.round(newSubtotal * 0.05);
        double totalAmount = newSubtotal + taxAmount + serviceFee - booking.getDiscountAmount();

        booking.setCheckedInDate(newCheckInDate);
        booking.setCheckedOutDate(newCheckOutDate);
        booking.setNumberOfNights(nights);
        booking.setSubtotal(newSubtotal);
        booking.setTaxAmount(taxAmount);
        booking.setServiceFee(serviceFee);
        booking.setTotalAmount(totalAmount);
        booking.setTotalPrice(totalAmount);

        return toBookingResponse(bookingRepository.save(booking));
    }

    @CacheEvict(value = {"bookings", "booking"}, allEntries = true)
    @Transactional
    @Auditable(
            action = "CANCEL",
            entity = "Booking",
            entityId = "#p0",
            detail = "'Cancelled booking ' + #p0"
    )
    @PublishDomainEvent(
            type = "BOOKING_CANCELLED",
            aggregate = "Booking",
            aggregateId = "#p0",
            payload = "#p0"
    )
    public void cancelBooking(Long bookingId) {
        Booking booking = findBookingByIdForUpdate(bookingId);
        validateCustomerAccess(booking.getCustomer());
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new AppException(ErrorCode.BOOKING_ALREADY_CANCELLED);
        }
        if (booking.getStatus() != BookingStatus.PENDING && booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new AppException(ErrorCode.BOOKING_CANNOT_CANCEL);
        }

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);
    }

    @CacheEvict(value = {"bookings", "booking", "rooms", "room"}, allEntries = true)
    @Transactional
    @Auditable(
            action = "CHECK_IN",
            entity = "Booking",
            entityId = "#result.bookingId",
            detail = "'Checked in booking ' + #result.bookingId"
    )
    @PublishDomainEvent(
            type = "BOOKING_CHECKED_IN",
            aggregate = "Booking",
            aggregateId = "#result.bookingId"
    )
    public BookingResponse checkIn(Long bookingId) {
        Booking booking = findBookingByIdForUpdate(bookingId);
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new AppException(ErrorCode.BOOKING_INVALID_STATUS_TRANSITION);
        }
        validateRoomBookable(booking.getRoom());

        booking.setStatus(BookingStatus.CHECKED_IN);
        booking.getRoom().setStatus(RoomStatus.OCCUPIED);
        roomRepository.save(booking.getRoom());

        return toBookingResponse(bookingRepository.save(booking));
    }

    @CacheEvict(value = {"bookings", "booking", "rooms", "room"}, allEntries = true)
    @Transactional
    @Auditable(
            action = "CHECK_OUT",
            entity = "Booking",
            entityId = "#result.bookingId",
            detail = "'Checked out booking ' + #result.bookingId"
    )
    @PublishDomainEvent(
            type = "BOOKING_CHECKED_OUT",
            aggregate = "Booking",
            aggregateId = "#result.bookingId"
    )
    public BookingResponse checkOut(Long bookingId) {
        Booking booking = findBookingByIdForUpdate(bookingId);
        if (booking.getStatus() != BookingStatus.CHECKED_IN) {
            throw new AppException(ErrorCode.BOOKING_INVALID_STATUS_TRANSITION);
        }

        booking.setStatus(BookingStatus.CHECKED_OUT);
        booking.getRoom().setStatus(RoomStatus.NEEDS_CLEANING);
        roomRepository.save(booking.getRoom());
        cleaningTaskService.createTaskForCheckout(booking.getRoom(), booking);

        return toBookingResponse(bookingRepository.save(booking));
    }

    private Room findRoomForBooking(BookingCreateRequest request, RoomType roomType, int quantity) {
        if (request.getRoomId() != null) {
            Room room = roomRepository.findByIdWithLock(request.getRoomId())
                    .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));
            validateRoomBookable(room);
            if (room.getType() != roomType) {
                throw new AppException(ErrorCode.INVALID_ROOM_TYPE);
            }
            if (bookingRepository.isRoomUnavailable(room.getRoomId(), request.getCheckInDate(), request.getCheckOutDate(), null))
                throw new AppException(ErrorCode.BOOKING_ROOM_UNAVAILABLE);
            ensureTypeQuantityAvailable(roomType, request.getCheckInDate(), request.getCheckOutDate(), quantity, null);
            return room;
        }

        ensureTypeQuantityAvailable(roomType, request.getCheckInDate(), request.getCheckOutDate(), quantity, null);
        return roomRepository.findAvailableRoomsByTypeForUpdate(roomType, request.getCheckInDate(), request.getCheckOutDate())
                .stream()
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_ROOM_UNAVAILABLE));
    }

    private void ensureTypeQuantityAvailable(
            RoomType roomType,
            LocalDate checkInDate,
            LocalDate checkOutDate,
            Integer quantity,
            Long excludeBookingId
    ) {
        int requestedQuantity = quantity == null ? 1 : quantity;
        int totalStock = roomRepository.findByTypeOrderByPrice(roomType).size();
        Long reservedQuantity = bookingRepository.sumReservedQuantityByType(
                roomType,
                checkInDate,
                checkOutDate,
                excludeBookingId
        );

        if (totalStock - reservedQuantity.intValue() < requestedQuantity) {
            throw new AppException(ErrorCode.BOOKING_ROOM_UNAVAILABLE);
        }
    }

    private void validateCapacity(RoomType roomType, int numberOfGuests, int quantity) {
        int capacity = switch (roomType) {
            case SINGLE -> 1;
            case DOUBLE -> 2;
            case VIP -> 4;
        };

        if (numberOfGuests > capacity * quantity) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
    }

    private Booking findBookingById(Long bookingId) {
        return bookingRepository.findActiveById(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));
    }

    private Booking findBookingByIdForUpdate(Long bookingId) {
        return bookingRepository.findActiveByIdForUpdate(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));
    }

    private void validateRoomBookable(Room room) {
        if (room.getStatus() == RoomStatus.MAINTENANCE) {
            throw new AppException(ErrorCode.ROOM_UNDER_MAINTENANCE);
        }
        if (room.getStatus() != RoomStatus.AVAILABLE) {
            throw new AppException(ErrorCode.ROOM_NOT_AVAILABLE);
        }
    }

    private BookingResponse toBookingResponse(Booking booking) {
        BookingResponse response = bookingMapper.toBookingResponse(booking);
        paymentRepository.findActiveByBookingId(response.getBookingId())
                .ifPresentOrElse(
                        payment -> response.setPaymentStatus(payment.getPaymentStatus().name()),
                        () -> response.setPaymentStatus("UNPAID")
                );
        if (booking.getCreatedAt() != null
                && ("UNPAID".equals(response.getPaymentStatus())
                || "PENDING".equals(response.getPaymentStatus())
                || "PROCESSING".equals(response.getPaymentStatus()))) {
            response.setPaymentExpiredAt(booking.getCreatedAt().plusMinutes(30));
        }
        return response;
    }

    private void validateCustomerAccess(Customer customer) {
        if (!hasRole("ROLE_USER")) {
            return;
        }
        if (customer.getUser() == null || !currentUsername().equals(customer.getUser().getUsername())) {
            throw new AppException(ErrorCode.CUSTOMER_ACCESS_DENIED);
        }
    }

    private String currentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private boolean hasRole(String role) {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                .stream()
                .anyMatch(authority -> authority.getAuthority().equals(role));
    }
}
