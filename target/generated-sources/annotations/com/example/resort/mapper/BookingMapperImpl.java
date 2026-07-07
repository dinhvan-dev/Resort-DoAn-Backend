package com.example.resort.mapper;

import com.example.resort.dto.response.BookingResponse;
import com.example.resort.entity.Booking;
import com.example.resort.entity.customer.Customer;
import com.example.resort.entity.room.Room;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-07-07T14:27:44+0700",
    comments = "version: 1.6.3, compiler: javac, environment: Java 21.0.10 (Oracle Corporation)"
)
@Component
public class BookingMapperImpl implements BookingMapper {

    @Override
    public BookingResponse toBookingResponse(Booking Booking) {
        if ( Booking == null ) {
            return null;
        }

        BookingResponse.BookingResponseBuilder bookingResponse = BookingResponse.builder();

        bookingResponse.customerId( bookingCustomerCustomerId( Booking ) );
        bookingResponse.fullName( bookingCustomerFullName( Booking ) );
        bookingResponse.roomId( bookingRoomRoomId( Booking ) );
        bookingResponse.roomNumber( bookingRoomRoomNumber( Booking ) );
        bookingResponse.checkedInDate( Booking.getCheckedInDate() );
        bookingResponse.checkedOutDate( Booking.getCheckedOutDate() );
        bookingResponse.bookingId( Booking.getBookingId() );
        if ( Booking.getStatus() != null ) {
            bookingResponse.status( Booking.getStatus().name() );
        }
        bookingResponse.totalPrice( Booking.getTotalPrice() );
        bookingResponse.createdAt( Booking.getCreatedAt() );

        return bookingResponse.build();
    }

    private String bookingCustomerCustomerId(Booking booking) {
        Customer customer = booking.getCustomer();
        if ( customer == null ) {
            return null;
        }
        return customer.getCustomerId();
    }

    private String bookingCustomerFullName(Booking booking) {
        Customer customer = booking.getCustomer();
        if ( customer == null ) {
            return null;
        }
        return customer.getFullName();
    }

    private Long bookingRoomRoomId(Booking booking) {
        Room room = booking.getRoom();
        if ( room == null ) {
            return null;
        }
        return room.getRoomId();
    }

    private String bookingRoomRoomNumber(Booking booking) {
        Room room = booking.getRoom();
        if ( room == null ) {
            return null;
        }
        return room.getRoomNumber();
    }
}
