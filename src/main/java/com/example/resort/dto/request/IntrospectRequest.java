package com.example.resort.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class IntrospectRequest {
    @NotBlank(message = "INVALID_TOKEN")
    private String token;
}
