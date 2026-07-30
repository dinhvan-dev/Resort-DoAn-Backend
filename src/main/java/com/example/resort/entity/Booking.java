package com.example.resort.entity;

import com.example.resort.entity.customer.Customer;
import com.example.resort.entity.room.Room;
import lombok.experimental.SuperBuilder;
import com.example.resort.enums.booking.BookingStatus;
import com.example.resort.enums.rooms.RoomType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(
        name = "bookings",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"roomId", "checkedInDate", "checkedOutDate"}
        )
)
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class Booking extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bookingId")
    private Long bookingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customerId", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "roomId")
    private Room room;

    @Enumerated(EnumType.STRING)
    @Column(name = "roomType", nullable = false)
    private RoomType roomType;

    @Column(name = "checkedInDate", nullable = false)
    private LocalDate checkedInDate;

    @Column(name = "checkedInTime", nullable = false)
    private LocalTime checkedInTime;

    @Column(name = "checkedOutDate", nullable = false)
    private LocalDate checkedOutDate;

    @Column(name = "checkedOutTime", nullable = false)
    private LocalTime checkedOutTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status;

    @Column(name = "totalPrice")
    private Double totalPrice;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "numberOfGuests", nullable = false)
    private Integer numberOfGuests;

    @Column(name = "pricePerNight")
    private Double pricePerNight;

    @Column(name = "numberOfNights")
    private Integer numberOfNights;

    @Column(name = "subtotal")
    private Double subtotal;

    @Column(name = "taxAmount", nullable = false)
    private Double taxAmount;

    @Column(name = "serviceFee", nullable = false)
    private Double serviceFee;

    @Column(name = "discountAmount", nullable = false)
    private Double discountAmount;

    @Column(name = "totalAmount")
    private Double totalAmount;
}
