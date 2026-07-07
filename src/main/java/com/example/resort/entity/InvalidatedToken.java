package com.example.resort.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.*;

import java.util.Date;

@Entity
@Table(name = "invalidatedTokes")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InvalidatedToken {
    @Id
    @Column(name = "token_id")
    private String tokenId;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "expiry_time")
    private Date expiryTime;
}
