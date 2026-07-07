package com.example.resort.dto.request;

import com.example.resort.enums.Role;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserRoleUpdateRequest {
    @NotNull(message = "Role is required")
    private Role role;
}
