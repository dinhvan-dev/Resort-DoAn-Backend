package com.example.resort.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthenticationResponse {
    private boolean  authenticated;
    private String token;
    private String refreshToken;
}
