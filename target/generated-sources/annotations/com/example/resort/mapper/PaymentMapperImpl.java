package com.example.resort.mapper;

import com.example.resort.dto.response.PaymentResponse;
import com.example.resort.entity.Booking;
import com.example.resort.entity.Payment;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-07-07T14:27:44+0700",
    comments = "version: 1.6.3, compiler: javac, environment: Java 21.0.10 (Oracle Corporation)"
)
@Component
public class PaymentMapperImpl implements PaymentMapper {

    @Override
    public PaymentResponse toPaymentResponse(Payment payment) {
        if ( payment == null ) {
            return null;
        }

        PaymentResponse.PaymentResponseBuilder paymentResponse = PaymentResponse.builder();

        paymentResponse.bookingId( paymentBookingBookingId( payment ) );
        paymentResponse.paymentId( payment.getPaymentId() );
        paymentResponse.paymentMethod( payment.getPaymentMethod() );
        paymentResponse.paymentStatus( payment.getPaymentStatus() );
        paymentResponse.amount( payment.getAmount() );
        paymentResponse.vnpayTransactionId( payment.getVnpayTransactionId() );
        paymentResponse.createdAt( payment.getCreatedAt() );
        paymentResponse.paidAt( payment.getPaidAt() );

        return paymentResponse.build();
    }

    private Long paymentBookingBookingId(Payment payment) {
        Booking booking = payment.getBooking();
        if ( booking == null ) {
            return null;
        }
        return booking.getBookingId();
    }
}
