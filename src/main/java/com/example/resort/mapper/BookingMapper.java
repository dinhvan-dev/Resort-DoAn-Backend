package com.example.resort.mapper;

import com.example.resort.dto.response.BookingResponse;
import com.example.resort.entity.Booking;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BookingMapper {
    @Mapping(source = "customer.customerId", target = "customerId")
    @Mapping(source = "customer.fullName",   target = "fullName")
    @Mapping(source = "room.roomId",          target = "roomId")
    @Mapping(source = "room.roomNumber",      target = "roomNumber")
    @Mapping(source = "checkedInDate",        target = "checkedInDate")
    @Mapping(source = "checkedOutDate",       target = "checkedOutDate")
    BookingResponse toBookingResponse(Booking Booking);
}
