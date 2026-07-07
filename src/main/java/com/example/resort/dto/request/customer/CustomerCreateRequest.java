package com.example.resort.dto.request.customer;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CustomerCreateRequest {
    private String fullName;

    @Pattern(regexp = "^$|^(0|\\+84)\\d{9}$", message = "Invalid phone number format")
    private String phoneNumber;

    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Identity number is required")
    @Pattern(regexp = "^\\d{12}$", message = "Identity number must be exactly 12 digits")
    private String identityNumber;
}
