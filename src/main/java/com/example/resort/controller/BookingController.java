package com.example.resort.controller;

import com.example.resort.dto.request.booking.BookingCreateRequest;
import com.example.resort.dto.response.ApiResponse;
import com.example.resort.dto.response.BookingResponse;
import com.example.resort.dto.response.PageResponse;
import com.example.resort.enums.booking.BookingStatus;
import com.example.resort.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping({"/bookings", "/Booking"})
@RequiredArgsConstructor
public class BookingController {
    private final BookingService bookingService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<BookingResponse> createBooking(@Valid @RequestBody BookingCreateRequest request) {
        return ApiResponse.<BookingResponse>builder()
                .result(bookingService.createBooking(request))
                .build();
    }

    @GetMapping
    public ApiResponse<PageResponse<BookingResponse>> getAllBookings(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size)
    {
        return ApiResponse.<PageResponse<BookingResponse>> builder()
                .result(bookingService.getAllBookings(page, size))
                .build();

    }

    @GetMapping("/my-bookings")
    public ApiResponse<List<BookingResponse>> getMyBookings()
    {
        return ApiResponse.<List<BookingResponse>> builder()
                .result(bookingService.getMyBookings())
                .build();
    }

    @GetMapping("/{bookingId}")
    public ApiResponse<BookingResponse> getBookingById(@PathVariable Long bookingId)
    {
        return ApiResponse.<BookingResponse> builder()
                .result(bookingService.getBookingById(bookingId))
                .build();
    }

    @GetMapping("/customer/{customerId}")
    public ApiResponse<List<BookingResponse>> getBookingsByCustomerId(@PathVariable String customerId)
    {
        return ApiResponse.<List<BookingResponse>> builder()
                .result(bookingService.getBookingsByCustomerId(customerId))
                .build();
    }

    @GetMapping("/room/{roomId}")
    public ApiResponse<List<BookingResponse>> getBookingsByRoomId(@PathVariable Long roomId)
    {
        return ApiResponse.<List<BookingResponse>> builder()
                .result(bookingService.getBookingsByRoomId(roomId))
                .build();
    }

    @GetMapping("/status")
    public ApiResponse<List<BookingResponse>> getBookingsByStatus(@RequestParam BookingStatus status)
    {
        return ApiResponse.<List<BookingResponse>> builder()
                .result(bookingService.getBookingsByStatus(status))
                .build();
    }

    @PutMapping("/{bookingId}/dates")
    public ApiResponse<BookingResponse> updateBookingDates(@PathVariable Long bookingId,
                                                           @RequestParam LocalDate newCheckInDate,
                                                           @RequestParam LocalDate newCheckOutDate)
    {
        return ApiResponse.<BookingResponse> builder()
                .result(bookingService.updateBookingDates(bookingId, newCheckInDate, newCheckOutDate))
                .build();
    }

    @PatchMapping("/{bookingId}/cancel")
    public ApiResponse<Void> cancelBooking(@PathVariable Long bookingId)
    {
        bookingService.cancelBooking(bookingId);
        return ApiResponse.<Void> builder()
                .build();
    }

    @PatchMapping("/{bookingId}/check-in")
    public ApiResponse<BookingResponse> checkIn(@PathVariable Long bookingId)
    {
        return ApiResponse.<BookingResponse> builder()
                .result(bookingService.checkIn(bookingId))
                .build();
    }

    @PatchMapping("/{bookingId}/check-out")
    public ApiResponse<BookingResponse> checkOut(@PathVariable Long bookingId)
    {
        return ApiResponse.<BookingResponse> builder()
                .result(bookingService.checkOut(bookingId))
                .build();
    }
}
