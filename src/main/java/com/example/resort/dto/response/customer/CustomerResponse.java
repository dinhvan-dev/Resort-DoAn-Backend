package com.example.resort.dto.response.customer;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CustomerResponse {
    private String customerId;
    private String fullName;
    private String phoneNumber;
    private String email;
    private String identityNumber;
    private String identityMasked;
    private LocalDate dateOfBirth;
    private String userId;
    private LocalDateTime createdAt;
}
