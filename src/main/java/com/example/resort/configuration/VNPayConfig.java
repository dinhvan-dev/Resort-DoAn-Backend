package com.example.resort.configuration;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class VNPayConfig {
    // lấy từ application.yml
    @Value("${vnpay.tmn-code}") // mã merchant VNPay cấp
    private String tmnCode;

    @Value("${vnpay.hash-secret}")  // Key bảo mật VNPay cấp
    private String hashSecret;

    @Value("${vnpay.payment-url}")  // URL sandbox VNpay
    private String paymentUrl;

    @Value("${vnpay.return-url}") // URL callback sau thanh toán
    private String returnUrl;

    @Value("${vnpay.api-url}")
    private String apiUrl;
}
