package com.example.resort.dto.response;

import com.example.resort.enums.Role;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserResponse {
    private String userId;
    private String username;
    private String email;
    private String fullName;
    private String phoneNumber;
    private Role role;
    private java.time.LocalDateTime createdAt;
    private java.time.LocalDateTime updatedAt;
    private boolean isActive;
}
