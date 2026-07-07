package com.example.resort.dto.request;

import com.example.resort.enums.Role;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserUpdateRequest {
    @Size(min = 8, message = "Username must be at least 8 characters long")
    private String username;

    @Size(min = 8, message = "Password must be at least 8 characters long")
    private String password;

    private Role role;

    private String email;
    private String fullName;
    private String phoneNumber;
    private Boolean isActive;
}
