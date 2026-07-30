package com.example.resort.service;

import com.example.resort.dto.response.BookingResponse;
import com.example.resort.dto.response.PaymentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
    public void sendBookingCreatedEmail(BookingResponse booking)
    {
        log.info("Sending booking created email for booking: {}", booking.getBookingId());
    }

    public void sendBookingCanceledEmail(Long bookingId)
    {
        log.info("Sending booking canceled email for booking: {}", bookingId);
    }

    public void sendCheckInEmail(BookingResponse booking)
    {
        log.info("Sending check-in email for booking: {}", booking.getBookingId());
    }

    public void sendCheckOutEmail(BookingResponse booking)
    {
        log.info("Sending check-out email for booking: {}", booking.getBookingId());
    }

    public void sendPaymentPaidEmail(PaymentResponse payment)
    {
        log.info("Sending payment paid email for payment: {}", payment.getPaymentId());
    }
}
