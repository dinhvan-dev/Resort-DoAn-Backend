package com.example.resort.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserSelfUpdateRequest {
    @NotBlank(message = "Full name must not be blank")
    private String fullName;

    @NotBlank(message = "Email must not be blank")
    @Email(message = "Email is invalid")
    private String email;

    @NotBlank(message = "Phone number must not be blank")
    @Pattern(regexp = "^(0|\\+84)\\d{9}$", message = "Invalid phone number format")
    private String phoneNumber;
}
