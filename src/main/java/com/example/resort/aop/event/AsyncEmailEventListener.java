package com.example.resort.aop.event;

import com.example.resort.dto.response.BookingResponse;
import com.example.resort.dto.response.PaymentResponse;
import com.example.resort.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AsyncEmailEventListener {
    private final EmailService emailService;

    @Async
    @EventListener
    public void handle(DomainEvent event) {
        switch (event.getType()) {
            case "BOOKING_CREATED" -> emailService.sendBookingCreatedEmail((BookingResponse) event.getPayload());

            case "BOOKING_CANCELLED" -> emailService.sendBookingCanceledEmail((Long) event.getPayload());

            case "BOOKING_CHECKED_IN" -> emailService.sendCheckInEmail((BookingResponse) event.getPayload());

            case "BOOKING_CHECKED_OUT" -> emailService.sendCheckOutEmail((BookingResponse) event.getPayload());

            case "PAYMENT_PAID" -> emailService.sendPaymentPaidEmail((PaymentResponse) event.getPayload());

            default -> {
            }
        }
    }
}
