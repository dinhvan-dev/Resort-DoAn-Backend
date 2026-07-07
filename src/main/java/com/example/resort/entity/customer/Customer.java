package com.example.resort.entity.customer;

import jakarta.persistence.*;
import lombok.*;

import com.example.resort.entity.BaseEntity;
import com.example.resort.entity.User;
import lombok.experimental.SuperBuilder;
@Entity
@Table(name = "customers")
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class Customer extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "customerId")
    private String customerId;

    @Column(name = "fullName", nullable = false)
    private String fullName;
    @Column(name = "phoneNumber", nullable = false, unique = true)
    private String phoneNumber;
    @Column(name = "email", nullable = false)
    private String email;
    @Column(name = "identityNumber", nullable = false, unique = true)
    private String identityNumber;

    @Column(name = "address")
    private String address;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private User user;
}
