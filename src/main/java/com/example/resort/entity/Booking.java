package com.example.resort.entity;

import com.example.resort.entity.customer.Customer;
import com.example.resort.entity.room.Room;
import lombok.experimental.SuperBuilder;
import com.example.resort.enums.booking.BookingStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

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
    @JoinColumn(name = "roomId", nullable = false)
    private Room room;

    @Column(name = "checkedInDate", nullable = false)
    private LocalDate checkedInDate;

    @Column(name = "checkedOutDate", nullable = false)
    private LocalDate checkedOutDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status;

    @Column(name = "totalPrice")
    private Double totalPrice;
}
