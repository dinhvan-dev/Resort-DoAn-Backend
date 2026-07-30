package com.example.resort.controller;

import com.example.resort.dto.request.PaymentRequest;
import com.example.resort.dto.request.PaymentCheckoutRequest;
import com.example.resort.dto.response.ApiResponse;
import com.example.resort.dto.response.PaymentCheckoutResponse;
import com.example.resort.dto.response.PaymentResponse;
import com.example.resort.dto.response.staff.StaffPaymentResponse;
import com.example.resort.service.StaffReportService;
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
    private final StaffReportService staffReportService;

    @PostMapping
    public ApiResponse<PaymentResponse> createPayment(@Valid @RequestBody PaymentRequest request, HttpServletRequest httpRequest)
    {
        return ApiResponse.<PaymentResponse> builder()
                .result(paymentService.createPayment(request, httpRequest))
                .build();
    }

    @PostMapping("/checkout")
    public ApiResponse<PaymentCheckoutResponse> createCheckout(@Valid @RequestBody PaymentCheckoutRequest request, HttpServletRequest httpRequest)
    {
        return ApiResponse.<PaymentCheckoutResponse> builder()
                .result(paymentService.createCheckout(request, httpRequest))
                .build();
    }

    @PatchMapping("/{paymentId}/confirm-cash")
    public ApiResponse<PaymentResponse> confirmCashPayment(@PathVariable String paymentId, HttpServletRequest httpRequest)
    {
        return ApiResponse.<PaymentResponse> builder()
                .result(paymentService.confirmCashPayment(paymentId))
                .build();
    }

    @PatchMapping("/{paymentId}/refund")
    public ApiResponse<PaymentResponse> refundPayment(@PathVariable String paymentId)
    {
        return ApiResponse.<PaymentResponse> builder()
                .result(paymentService.refundPayment(paymentId))
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
    public ApiResponse<PaymentResponse> getPaymentByBookingId(@PathVariable Long bookingId, HttpServletRequest httpRequest)
    {
        return ApiResponse.<PaymentResponse> builder()
                .result(paymentService.getPaymentByBookingId(bookingId, httpRequest))
                .build();
    }

    @GetMapping("/cash-pending")
    public ApiResponse<java.util.List<StaffPaymentResponse>> getCashPendingPayments()
    {
        return ApiResponse.<java.util.List<StaffPaymentResponse>> builder()
                .result(staffReportService.getCashPendingPayments())
                .build();
    }

    @GetMapping("/staff")
    public ApiResponse<java.util.List<StaffPaymentResponse>> getStaffPayments()
    {
        return ApiResponse.<java.util.List<StaffPaymentResponse>> builder()
                .result(staffReportService.getStaffPayments())
                .build();
    }

}
