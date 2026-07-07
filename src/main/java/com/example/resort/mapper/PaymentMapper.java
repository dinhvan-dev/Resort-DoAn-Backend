package com.example.resort.mapper;

import com.example.resort.dto.response.PaymentResponse;
import com.example.resort.entity.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentMapper {
    @Mapping(source = "booking.bookingId", target = "bookingId")
    @Mapping(target = "paymentUrl", ignore = true)
    PaymentResponse toPaymentResponse(Payment payment);
}
