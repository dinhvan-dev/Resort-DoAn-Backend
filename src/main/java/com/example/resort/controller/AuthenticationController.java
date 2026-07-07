package com.example.resort.controller;

import com.example.resort.dto.request.AuthenticationRequest;
import com.example.resort.dto.request.IntrospectRequest;
import com.example.resort.dto.request.RefreshTokenRequest;
import com.example.resort.dto.response.ApiResponse;
import com.example.resort.dto.response.AuthenticationResponse;
import com.example.resort.dto.response.IntrospectResponse;
import com.example.resort.service.AuthenticationService;
import com.nimbusds.jose.JOSEException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.ParseException;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthenticationController {
    private final AuthenticationService authenticationService;

    @PostMapping("/token")
    public ApiResponse<AuthenticationResponse> authenticated(@RequestBody AuthenticationRequest request)
    {
        return ApiResponse.<AuthenticationResponse> builder()
                .result(authenticationService.authenticated(request))
                .build();
    }

    @PostMapping("/introspect")
    public ApiResponse<IntrospectResponse> introspect(@RequestBody IntrospectRequest request)
            throws JOSEException, ParseException
    {
        return ApiResponse.<IntrospectResponse> builder()
                .result(authenticationService.introspect(request))
                .build();
    }
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestBody IntrospectRequest request)
        throws ParseException
    {
        authenticationService.logout(request);
        return ApiResponse.<Void> builder()
                .message("Logged Out successfully")
                .build();
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthenticationResponse> refresh(@Valid @RequestBody RefreshTokenRequest request)
    {
        return ApiResponse.<AuthenticationResponse> builder()
                .result(authenticationService.refreshToken(request))
                .build();
    }


}
