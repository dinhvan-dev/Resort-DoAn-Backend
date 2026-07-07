package com.example.resort.controller;

import com.example.resort.dto.request.PaymentRequest;
import com.example.resort.dto.response.ApiResponse;
import com.example.resort.dto.response.PaymentResponse;
import com.example.resort.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;

    @PostMapping
    public ApiResponse<PaymentResponse> createPayment(@Valid @RequestBody PaymentRequest request, HttpServletRequest httpRequest)
    {
        return ApiResponse.<PaymentResponse> builder()
                .result(paymentService.createPayment(request, httpRequest))
                .build();
    }

    @PatchMapping("/{paymentId}/confirm-cash")
    public ApiResponse<PaymentResponse> confirmCashPayment(@PathVariable String paymentId, HttpServletRequest httpRequest)
    {
        return ApiResponse.<PaymentResponse> builder()
                .result(paymentService.confirmCashPayment(paymentId))
                .build();
    }

    @GetMapping("/vnpay-callback")
    public ApiResponse<PaymentResponse> vanpayCallback(@RequestParam Map<String, String> params)
    {
        return ApiResponse.<PaymentResponse> builder()
                .result(paymentService.handleVNPayCallback(params))
                .build();
    }

    @GetMapping("/booking/{bookingId}")
    public ApiResponse<PaymentResponse> getPaymentByBookingId(@PathVariable Long bookingId)
    {
        return ApiResponse.<PaymentResponse> builder()
                .result(paymentService.getPaymentByBookingId(bookingId))
                .build();
    }

}
