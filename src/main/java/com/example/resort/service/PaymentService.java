package com.example.resort.service;

import com.example.resort.configuration.VNPayConfig;
import com.example.resort.dto.request.PaymentRequest;
import com.example.resort.dto.response.PaymentResponse;
import com.example.resort.entity.Booking;
import com.example.resort.entity.Payment;
import com.example.resort.enums.PaymentMethod;
import com.example.resort.enums.PaymentStatus;
import com.example.resort.enums.booking.BookingStatus;
import com.example.resort.exception.AppException;
import com.example.resort.exception.ErrorCode;
import com.example.resort.mapper.PaymentMapper;
import com.example.resort.repository.BookingRepository;
import com.example.resort.repository.PaymentRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final PaymentMapper paymentMapper;
    private final VNPayConfig vnPayConfig;

    @Transactional
    public PaymentResponse createPayment(PaymentRequest request, HttpServletRequest httpRequest)
    {
        // tìm booking
        Booking booking = bookingRepository.findActiveById(request.getBookingId())
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        validateBookingAccess(booking);
        validatePayableBooking(booking);

        // kiểm tra đã có Payment hay chưa
        if (paymentRepository.findActiveByBookingId(booking.getBookingId()).isPresent())
        {
            throw new AppException(ErrorCode.PAYMENT_ALREADY_EXISTS);
        }

        // tạo payment record
        Payment payment = Payment.builder()
                .booking(booking)
                .paymentMethod(request.getPaymentMethod())
                .paymentStatus(PaymentStatus.PENDING)
                .amount(booking.getTotalPrice())
                .build();

        payment = paymentRepository.save(payment);

        // xử lý theo phương tức thanh toán
        if (request.getPaymentMethod() == PaymentMethod.VNPAY)
        {
            // Tạo URL redirect VNPay
            String paymentUrl = createVNPayUrl(payment, httpRequest);
            payment.setVnpayOrderInfo("thanh toan booking#" + booking.getBookingId());
            paymentRepository.save(payment);

            PaymentResponse response = paymentMapper.toPaymentResponse(payment);
            response.setPaymentUrl(paymentUrl);
            return response;
        }
        else
        {
            // CASH Staff xác nhận thành công
            PaymentResponse response = paymentMapper.toPaymentResponse(payment);
            response.setPaymentUrl(null);
            return response;
        }


    }

    private String createVNPayUrl(Payment payment, HttpServletRequest request)
    {
        String vnpVersion = "2.1.0";
        String vnpCommand = "pay";
        String CurrCode = "VND";
        String vnpLocale = "vn";

        // số tiền * 100 (VNPay  tính theo đơn vị nhỏ nhất)
        long amount = (long) (payment.getAmount() * 100);

        Map<String, String> vnpParams = new TreeMap<>();
        vnpParams.put("vnp_Version", vnpVersion);
        vnpParams.put("vnp_Command", vnpCommand);
        vnpParams.put("vnp_TmnCode", vnPayConfig.getTmnCode());
        vnpParams.put("vnp_Amount", String.valueOf(amount));
        vnpParams.put("vnp_CurrCode", CurrCode);
        vnpParams.put("vnp_TxnRef", String.valueOf(payment.getBooking().getBookingId()));
        vnpParams.put("vnp_OrderInfo", "Thanh toan booking #" + payment.getBooking().getBookingId());
        vnpParams.put("vnp_OrderType", "other");
        vnpParams.put("vnp_Locale", vnpLocale);
        vnpParams.put("vnp_ReturnUrl", vnPayConfig.getReturnUrl());
        vnpParams.put("vnp_IpAddr", getClientIp(request));
        vnpParams.put("vnp_CreateDate", new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));

        // tạo chữ ký
        String queryString = buildQueryString(vnpParams);
        String signature = createSignature(vnpParams);

        return vnPayConfig.getPaymentUrl() + "?" + queryString + "&vnp_SecureHash=" + signature;
    }

    // lấy Ip từ Client
    private String getClientIp(HttpServletRequest request)
    {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty())
        {
            ip = request.getRemoteAddr();
        }
        return ip.split(",")[0].trim();
    }

    private String buildQueryString(Map<String, String> params)
    {
        StringBuilder query = new StringBuilder();
        params.forEach((key, value) -> {
            if (query.length() > 0) query.append("&");
            query.append(URLEncoder.encode(key, StandardCharsets.UTF_8))
                    .append("=")
                    .append(URLEncoder.encode(value, StandardCharsets.UTF_8));
        });
        return query.toString();
    }

    // tạo chữ ký HMAC-SHA512
    private String createSignature(Map<String, String> params)
    {
        try
        {
            String data = buildQueryString(new TreeMap<>(params));
            Mac hmac = Mac.getInstance("HMacSHA512");
            hmac.init(new SecretKeySpec(
                    vnPayConfig.getHashSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA512"
            ));
            byte[] hash = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();

        }
        catch (Exception e)
        {
            throw new RuntimeException("Cannot create VNPay signature", e);
        }
    }

    // Staff xác nhận thanh toán tiền mặt
    @Transactional
    public PaymentResponse confirmCashPayment(String paymentId)
    {
        Payment payment = paymentRepository.findActiveById(paymentId)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));

        if (payment.getPaymentMethod() != PaymentMethod.CASH)
            throw new AppException(ErrorCode.INVALID_PAYMENT_METHOD);

        if (payment.getPaymentStatus() != PaymentStatus.PENDING)
            throw new AppException(ErrorCode.PAYMENT_ALREADY_PROCESSED);

        // cập nhật payment
        payment.setPaymentStatus(PaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);

        // A paid booking is confirmed. Check-in should be a separate booking action.
        Booking booking = payment.getBooking();
        booking.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);

        log.info("Cash payment confirmed for booking: {}", booking.getBookingId());
        return paymentMapper.toPaymentResponse(payment);
    }

    // VNPay callback - nhận kết quả t VNPay
    @Transactional
    public PaymentResponse handleVNPayCallback(Map<String, String> params)
    {
        // Verify chữ ký từ VNPay
        String vnpSecureHash = params.get("vnp_SecureHash");
        params.remove("vnp_SecureHash");
        params.remove("vnp_SecureHashType");

        String sigValue = createSignature(params);

        if (!sigValue.equals(vnpSecureHash))
        {
            log.error("VNPay signature invalid");
            throw new AppException(ErrorCode.PAYMENT_INVALID_SIGNATURE);
        }

        String transactionId = params.get("vnp_TransactionNo");
        String responseCode = params.get("vnp_ResponseCode");
        String bookingIdStr = params.get("vnp_TxnRef");

        Payment payment = paymentRepository.findActiveByBookingId(Long.parseLong(bookingIdStr))
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));

        payment.setVnpayTransactionId(transactionId);

        if ("00".equals(responseCode))
        {
            // thanh toán thành công
            payment.setPaymentStatus(PaymentStatus.PAID);
            payment.setPaidAt(LocalDateTime.now());

            // cập nhật booking -> CONFIRMED
            Booking booking = payment.getBooking();
            booking.setStatus(BookingStatus.CONFIRMED);
            bookingRepository.save(booking);

            log.info("VNPay payment success - booking: {}", bookingIdStr);
        }
        else
        {
            // thanh toán thất bại
            payment.setPaymentStatus(PaymentStatus.FAILED);
            log.warn("VNPay payment failed - responseCode: {}", responseCode);
        }

        paymentRepository.save(payment);
        return paymentMapper.toPaymentResponse(payment);
    }

    // lấy thông tin payment theo bookingId
    @Transactional
    public PaymentResponse getPaymentByBookingId(Long bookingId)
    {
        Payment payment = paymentRepository.findActiveByBookingId(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));
        validateBookingAccess(payment.getBooking());
        return paymentMapper.toPaymentResponse(payment);
    }

    private void validatePayableBooking(Booking booking)
    {
        if (booking.getStatus() != BookingStatus.PENDING && booking.getStatus() != BookingStatus.CONFIRMED)
        {
            throw new AppException(ErrorCode.BOOKING_INVALID_STATUS_TRANSITION);
        }
    }

    private void validateBookingAccess(Booking booking)
    {
        if (!hasRole("ROLE_USER"))
        {
            return;
        }

        if (booking.getCustomer() == null
                || booking.getCustomer().getUser() == null
                || !currentUsername().equals(booking.getCustomer().getUser().getUsername()))
        {
            throw new AppException(ErrorCode.CUSTOMER_ACCESS_DENIED);
        }
    }

    private String currentUsername()
    {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private boolean hasRole(String role)
    {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                .stream()
                .anyMatch(authority -> authority.getAuthority().equals(role));
    }
}
