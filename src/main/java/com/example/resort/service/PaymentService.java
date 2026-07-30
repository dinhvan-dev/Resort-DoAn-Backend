package com.example.resort.service;

import com.example.resort.aop.logging.Auditable;
import com.example.resort.aop.event.PublishDomainEvent;
import com.example.resort.configuration.VNPayConfig;
import com.example.resort.dto.request.PaymentCheckoutRequest;
import com.example.resort.dto.request.PaymentRequest;
import com.example.resort.dto.response.PaymentCheckoutResponse;
import com.example.resort.dto.response.PaymentResponse;
import com.example.resort.entity.Booking;
import com.example.resort.entity.Payment;
import com.example.resort.entity.PaymentGroup;
import com.example.resort.enums.PaymentMethod;
import com.example.resort.enums.PaymentStatus;
import com.example.resort.enums.booking.BookingStatus;
import com.example.resort.exception.AppException;
import com.example.resort.exception.ErrorCode;
import com.example.resort.mapper.PaymentMapper;
import com.example.resort.repository.BookingRepository;
import com.example.resort.repository.PaymentGroupRepository;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {
    private static final int VNPAY_PAYMENT_TTL_MINUTES = 15;
    private static final DateTimeFormatter VNPAY_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final PaymentRepository paymentRepository;
    private final PaymentGroupRepository paymentGroupRepository;
    private final BookingRepository bookingRepository;
    private final PaymentMapper paymentMapper;
    private final VNPayConfig vnPayConfig;

    @Transactional
    @Auditable(
            action = "CREATE",
            entity = "Payment",
            entityId = "#result.paymentId",
            detail = "'Created payment for booking ' + #result.bookingId + ' amount ' + #result.amount"
    )
    @PublishDomainEvent(
            type = "PAYMENT_CREATED",
            aggregate = "Payment",
            aggregateId = "#result.paymentId"
    )
    public PaymentResponse createPayment(PaymentRequest request, HttpServletRequest httpRequest)
    {
        Booking booking = bookingRepository.findActiveById(request.getBookingId())
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        validateBookingAccess(booking);
        validatePayableBooking(booking);

        var existingPayment = paymentRepository.findActiveByBookingId(booking.getBookingId());
        if (existingPayment.isPresent())
        {
            return reuseExistingPayment(existingPayment.get(), request.getPaymentMethod(), httpRequest);
        }

        Payment payment = Payment.builder()
                .booking(booking)
                .paymentMethod(request.getPaymentMethod())
                .paymentStatus(initialPaymentStatus(request.getPaymentMethod()))
                .amount(getBookingAmount(booking))
                .paymentExpiredAt(expiresAtFor(request.getPaymentMethod()))
                .build();

        if (request.getPaymentMethod() == PaymentMethod.VNPAY)
        {
            payment.setVnpayOrderInfo("thanh toan booking#" + booking.getBookingId());
        }

        payment = paymentRepository.save(payment);
        return toPaymentResponseWithUrl(payment, httpRequest);
    }

    @Transactional
    @Auditable(
            action = "CREATE",
            entity = "PaymentGroup",
            entityId = "#result.paymentGroupId",
            detail = "'Created checkout for bookings ' + #result.bookingIds"
    )
    @PublishDomainEvent(
            type = "PAYMENT_CHECKOUT_CREATED",
            aggregate = "PaymentGroup",
            aggregateId = "#result.paymentGroupId"
    )
    public PaymentCheckoutResponse createCheckout(PaymentCheckoutRequest request, HttpServletRequest httpRequest)
    {
        if (request.getPaymentMethod() != PaymentMethod.VNPAY)
        {
            throw new AppException(ErrorCode.INVALID_PAYMENT_METHOD);
        }

        List<Long> bookingIds = request.getBookingIds().stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (bookingIds.isEmpty())
        {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        List<Booking> bookings = bookingIds.stream()
                .map(bookingId -> bookingRepository.findActiveById(bookingId)
                        .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND)))
                .toList();

        bookings.forEach(booking -> {
            validateBookingAccess(booking);
            validatePayableBooking(booking);
        });

        List<Payment> existingPayments = paymentRepository.findActiveByBookingIds(bookingIds);
        if (!existingPayments.isEmpty())
        {
            PaymentCheckoutResponse existingCheckout = resolveExistingCheckout(bookingIds, existingPayments, httpRequest);
            if (existingCheckout != null)
            {
                return existingCheckout;
            }

            return createCheckoutFromReusablePayments(
                    bookings,
                    existingPayments,
                    request.getPaymentMethod(),
                    httpRequest
            );
        }

        return createNewCheckout(bookings, request.getPaymentMethod(), httpRequest);
    }

    private PaymentCheckoutResponse createNewCheckout(
            List<Booking> bookings,
            PaymentMethod paymentMethod,
            HttpServletRequest httpRequest
    )
    {
        List<Long> bookingIds = bookings.stream().map(Booking::getBookingId).toList();
        double amount = bookings.stream()
                .mapToDouble(this::getBookingAmount)
                .sum();

        PaymentGroup paymentGroup = PaymentGroup.builder()
                .paymentMethod(paymentMethod)
                .paymentStatus(PaymentStatus.PROCESSING)
                .amount(amount)
                .vnpayOrderInfo("Thanh toan " + bookingIds.size() + " bookings")
                .paymentExpiredAt(expiresAtFor(paymentMethod))
                .build();
        paymentGroup = paymentGroupRepository.save(paymentGroup);

        PaymentGroup savedPaymentGroup = paymentGroup;
        List<Payment> paymentDrafts = new ArrayList<>();
        bookings.forEach(booking -> paymentDrafts.add(Payment.builder()
                .booking(booking)
                .paymentGroup(savedPaymentGroup)
                .paymentMethod(paymentMethod)
                .paymentStatus(PaymentStatus.PROCESSING)
                .amount(getBookingAmount(booking))
                .vnpayOrderInfo("thanh toan booking#" + booking.getBookingId())
                .paymentExpiredAt(savedPaymentGroup.getPaymentExpiredAt())
                .build()));
        List<Payment> payments = paymentRepository.saveAll(paymentDrafts);

        String paymentUrl = createVNPayUrl(savedPaymentGroup, httpRequest);
        return toPaymentCheckoutResponse(savedPaymentGroup, payments, paymentUrl);
    }

    @Transactional
    @Auditable(
            action = "CONFIRM_CASH",
            entity = "Payment",
            entityId = "#result.paymentId",
            detail = "'Confirmed cash payment ' + #result.paymentId + ' for booking ' + #result.bookingId"
    )
    @PublishDomainEvent(
            type = "PAYMENT_PAID",
            aggregate = "Payment",
            aggregateId = "#result.paymentId",
            condition = "#result.paymentStatus.name() == 'PAID'"
    )
    public PaymentResponse confirmCashPayment(String paymentId)
    {
        Payment payment = paymentRepository.findActiveByIdForUpdate(paymentId)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));

        if (payment.getPaymentMethod() != PaymentMethod.CASH)
        {
            throw new AppException(ErrorCode.INVALID_PAYMENT_METHOD);
        }

        if (payment.getPaymentStatus() != PaymentStatus.PENDING)
        {
            throw new AppException(ErrorCode.PAYMENT_ALREADY_PROCESSED);
        }

        payment.setPaymentStatus(PaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);

        Booking booking = payment.getBooking();
        booking.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);

        log.info("Cash payment confirmed for booking: {}", booking.getBookingId());
        return paymentMapper.toPaymentResponse(payment);
    }

    @Transactional
    @Auditable(
            action = "REFUND",
            entity = "Payment",
            entityId = "#result.paymentId",
            detail = "'Refunded payment ' + #result.paymentId + ' for booking ' + #result.bookingId"
    )
    @PublishDomainEvent(
            type = "PAYMENT_REFUNDED",
            aggregate = "Payment",
            aggregateId = "#result.paymentId"
    )
    public PaymentResponse refundPayment(String paymentId)
    {
        Payment payment = paymentRepository.findActiveByIdForUpdate(paymentId)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));

        if (payment.getPaymentStatus() != PaymentStatus.PAID)
        {
            throw new AppException(ErrorCode.PAYMENT_CANNOT_REFUND);
        }

        payment.setPaymentStatus(PaymentStatus.REFUNDED);
        paymentRepository.save(payment);
        updatePaymentGroupRefundStatus(payment.getPaymentGroup());

        log.info("Payment refunded: {}", payment.getPaymentId());
        return paymentMapper.toPaymentResponse(payment);
    }

    @Transactional
    @Auditable(
            action = "VNPAY_CALLBACK",
            entity = "Payment",
            entityId = "#result.paymentId",
            detail = "'Processed VNPAY callback for payment ' + #result.paymentId + ' status ' + #result.paymentStatus"
    )
    @PublishDomainEvent(
            type = "PAYMENT_PAID",
            aggregate = "Payment",
            aggregateId = "#result.paymentId",
            condition = "#result.paymentStatus.name() == 'PAID'"
    )
    public PaymentResponse handleVNPayCallback(Map<String, String> params)
    {
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
        String txnRef = params.get("vnp_TxnRef");

        if (transactionId != null) {
            var processedPayment = paymentRepository.findActiveByVnpayTransactionId(transactionId);
            if (processedPayment.isPresent() && isFinalStatus(processedPayment.get().getPaymentStatus())) {
                return paymentMapper.toPaymentResponse(processedPayment.get());
            }

            var processedGroup = paymentGroupRepository.findActiveByVnpayTransactionId(transactionId);
            if (processedGroup.isPresent() && isFinalStatus(processedGroup.get().getPaymentStatus())) {
                return firstPaymentResponse(processedGroup.get());
            }
        }

        PaymentGroup paymentGroup = paymentGroupRepository.findActiveById(txnRef).orElse(null);
        if (paymentGroup != null)
        {
            return handleVNPayGroupCallback(paymentGroup, transactionId, responseCode);
        }

        Payment payment = paymentRepository.findActiveByBookingId(parseBookingTxnRef(txnRef))
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));
        if (isFinalStatus(payment.getPaymentStatus()))
        {
            return paymentMapper.toPaymentResponse(payment);
        }
        payment.setVnpayTransactionId(transactionId);

        if ("00".equals(responseCode))
        {
            payment.setPaymentStatus(PaymentStatus.PAID);
            payment.setPaidAt(LocalDateTime.now());

            Booking booking = payment.getBooking();
            booking.setStatus(BookingStatus.CONFIRMED);
            bookingRepository.save(booking);

            log.info("VNPay payment success - booking: {}", txnRef);
        }
        else
        {
            payment.setPaymentStatus(PaymentStatus.FAILED);
            log.warn("VNPay payment failed - responseCode: {}", responseCode);
        }

        paymentRepository.save(payment);
        return paymentMapper.toPaymentResponse(payment);
    }

    @Transactional
    public PaymentResponse getPaymentByBookingId(Long bookingId, HttpServletRequest httpRequest)
    {
        Payment payment = paymentRepository.findActiveByBookingId(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));
        validateBookingAccess(payment.getBooking());
        expirePaymentIfNeeded(payment);

        PaymentResponse response = paymentMapper.toPaymentResponse(payment);
        if (payment.getPaymentGroup() != null
                && payment.getPaymentGroup().getPaymentMethod() == PaymentMethod.VNPAY
                && isPaymentWaiting(payment.getPaymentGroup().getPaymentStatus())
                && !isExpired(payment.getPaymentGroup().getPaymentExpiredAt()))
        {
            response.setPaymentUrl(createVNPayUrl(payment.getPaymentGroup(), httpRequest));
        }
        else if (payment.getPaymentMethod() == PaymentMethod.VNPAY
                && isPaymentWaiting(payment.getPaymentStatus())
                && !isExpired(payment.getPaymentExpiredAt()))
        {
            response.setPaymentUrl(createVNPayUrl(payment, httpRequest));
        }

        return response;
    }

    private PaymentCheckoutResponse resolveExistingCheckout(
            List<Long> bookingIds,
            List<Payment> existingPayments,
            HttpServletRequest httpRequest
    )
    {
        PaymentGroup paymentGroup = existingPayments.get(0).getPaymentGroup();
        if (paymentGroup == null)
        {
            return null;
        }

        boolean sameGroup = existingPayments.stream()
                .allMatch(payment -> payment.getPaymentGroup() != null
                        && payment.getPaymentGroup().getPaymentGroupId().equals(paymentGroup.getPaymentGroupId()));
        if (!sameGroup)
        {
            return null;
        }

        List<Payment> groupPayments = paymentRepository.findActiveByPaymentGroupId(paymentGroup.getPaymentGroupId());
        Set<Long> requestedIds = new LinkedHashSet<>(bookingIds);
        Set<Long> groupBookingIds = groupPayments.stream()
                .map(payment -> payment.getBooking().getBookingId())
                .collect(LinkedHashSet::new, Set::add, Set::addAll);

        if (!requestedIds.equals(groupBookingIds))
        {
            return null;
        }

        String paymentUrl = paymentGroup.getPaymentMethod() == PaymentMethod.VNPAY
                && isPaymentWaiting(paymentGroup.getPaymentStatus())
                && !isExpired(paymentGroup.getPaymentExpiredAt())
                ? createVNPayUrl(paymentGroup, httpRequest)
                : null;
        return toPaymentCheckoutResponse(paymentGroup, groupPayments, paymentUrl);
    }

    private PaymentCheckoutResponse createCheckoutFromReusablePayments(
            List<Booking> bookings,
            List<Payment> existingPayments,
            PaymentMethod paymentMethod,
            HttpServletRequest httpRequest
    )
    {
        boolean canReuse = existingPayments.stream()
                .allMatch(this::canReuseForCheckout);
        if (!canReuse)
        {
            throw new AppException(ErrorCode.PAYMENT_ALREADY_PROCESSED);
        }

        List<Long> bookingIds = bookings.stream().map(Booking::getBookingId).toList();
        double amount = bookings.stream()
                .mapToDouble(this::getBookingAmount)
                .sum();

        PaymentGroup paymentGroup = PaymentGroup.builder()
                .paymentMethod(paymentMethod)
                .paymentStatus(PaymentStatus.PROCESSING)
                .amount(amount)
                .vnpayOrderInfo("Thanh toan " + bookingIds.size() + " bookings")
                .paymentExpiredAt(expiresAtFor(paymentMethod))
                .build();
        paymentGroup = paymentGroupRepository.save(paymentGroup);

        PaymentGroup savedPaymentGroup = paymentGroup;
        List<Payment> checkoutPayments = new ArrayList<>();
        bookings.forEach(booking -> {
            Payment payment = existingPayments.stream()
                    .filter(existingPayment -> existingPayment.getBooking().getBookingId().equals(booking.getBookingId()))
                    .findFirst()
                    .orElseGet(() -> Payment.builder()
                            .booking(booking)
                            .paymentMethod(paymentMethod)
                            .build());

            payment.setPaymentGroup(savedPaymentGroup);
            payment.setPaymentMethod(paymentMethod);
            payment.setPaymentStatus(PaymentStatus.PROCESSING);
            payment.setAmount(getBookingAmount(booking));
            payment.setPaidAt(null);
            payment.setVnpayTransactionId(null);
            payment.setVnpayOrderInfo("thanh toan booking#" + booking.getBookingId());
            payment.setPaymentExpiredAt(savedPaymentGroup.getPaymentExpiredAt());
            checkoutPayments.add(payment);
        });

        List<Payment> payments = paymentRepository.saveAll(checkoutPayments);
        String paymentUrl = createVNPayUrl(savedPaymentGroup, httpRequest);
        return toPaymentCheckoutResponse(savedPaymentGroup, payments, paymentUrl);
    }

    private boolean canReuseForCheckout(Payment payment)
    {
        return payment.getPaymentStatus() != PaymentStatus.PAID
                && payment.getPaymentStatus() != PaymentStatus.REFUNDED;
    }

    private PaymentResponse handleVNPayGroupCallback(
            PaymentGroup paymentGroup,
            String transactionId,
            String responseCode
    )
    {
        List<Payment> payments = paymentRepository.findActiveByPaymentGroupId(paymentGroup.getPaymentGroupId());
        if (isFinalStatus(paymentGroup.getPaymentStatus()))
        {
            return payments.stream()
                    .findFirst()
                    .map(paymentMapper::toPaymentResponse)
                    .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));
        }
        paymentGroup.setVnpayTransactionId(transactionId);

        if ("00".equals(responseCode))
        {
            paymentGroup.setPaymentStatus(PaymentStatus.PAID);
            paymentGroup.setPaidAt(LocalDateTime.now());

            payments.forEach(payment -> {
                payment.setVnpayTransactionId(transactionId);
                payment.setPaymentStatus(PaymentStatus.PAID);
                payment.setPaidAt(paymentGroup.getPaidAt());

                Booking booking = payment.getBooking();
                booking.setStatus(BookingStatus.CONFIRMED);
                bookingRepository.save(booking);
            });

            log.info("VNPay payment success - group: {}", paymentGroup.getPaymentGroupId());
        }
        else
        {
            paymentGroup.setPaymentStatus(PaymentStatus.FAILED);
            payments.forEach(payment -> {
                payment.setVnpayTransactionId(transactionId);
                payment.setPaymentStatus(PaymentStatus.FAILED);
            });
            log.warn("VNPay payment group failed - responseCode: {}", responseCode);
        }

        paymentGroupRepository.save(paymentGroup);
        paymentRepository.saveAll(payments);
        return payments.stream()
                .findFirst()
                .map(paymentMapper::toPaymentResponse)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));
    }

    private PaymentCheckoutResponse toPaymentCheckoutResponse(
            PaymentGroup paymentGroup,
            List<Payment> payments,
            String paymentUrl
    )
    {
        return PaymentCheckoutResponse.builder()
                .paymentGroupId(paymentGroup.getPaymentGroupId())
                .bookingIds(payments.stream().map(payment -> payment.getBooking().getBookingId()).toList())
                .paymentMethod(paymentGroup.getPaymentMethod())
                .paymentStatus(paymentGroup.getPaymentStatus())
                .amount(paymentGroup.getAmount())
                .paymentUrl(paymentUrl)
                .vnpayTransactionId(paymentGroup.getVnpayTransactionId())
                .createdAt(paymentGroup.getCreatedAt())
                .paymentExpiredAt(paymentGroup.getPaymentExpiredAt())
                .paidAt(paymentGroup.getPaidAt())
                .payments(payments.stream().map(paymentMapper::toPaymentResponse).toList())
                .build();
    }

    private void updatePaymentGroupRefundStatus(PaymentGroup paymentGroup)
    {
        if (paymentGroup == null)
        {
            return;
        }

        List<Payment> groupPayments = paymentRepository.findActiveByPaymentGroupId(paymentGroup.getPaymentGroupId());
        boolean allRefunded = groupPayments.stream()
                .allMatch(payment -> payment.getPaymentStatus() == PaymentStatus.REFUNDED);
        boolean anyRefunded = groupPayments.stream()
                .anyMatch(payment -> payment.getPaymentStatus() == PaymentStatus.REFUNDED
                        || payment.getPaymentStatus() == PaymentStatus.PARTIALLY_REFUNDED);

        if (allRefunded)
        {
            paymentGroup.setPaymentStatus(PaymentStatus.REFUNDED);
        }
        else if (anyRefunded)
        {
            paymentGroup.setPaymentStatus(PaymentStatus.PARTIALLY_REFUNDED);
        }

        paymentGroupRepository.save(paymentGroup);
    }

    private PaymentResponse reuseExistingPayment(Payment payment, PaymentMethod paymentMethod, HttpServletRequest httpRequest)
    {
        expirePaymentIfNeeded(payment);
        if (payment.getPaymentStatus() == PaymentStatus.PAID
                || payment.getPaymentStatus() == PaymentStatus.REFUNDED
                || payment.getPaymentStatus() == PaymentStatus.PARTIALLY_REFUNDED)
        {
            throw new AppException(ErrorCode.PAYMENT_ALREADY_PROCESSED);
        }

        payment.setPaymentGroup(null);
        payment.setPaymentMethod(paymentMethod);
        payment.setPaymentStatus(initialPaymentStatus(paymentMethod));
        payment.setAmount(getBookingAmount(payment.getBooking()));
        payment.setPaidAt(null);
        payment.setVnpayTransactionId(null);
        payment.setVnpayOrderInfo(paymentMethod == PaymentMethod.VNPAY
                ? "thanh toan booking#" + payment.getBooking().getBookingId()
                : null);
        payment.setPaymentExpiredAt(expiresAtFor(paymentMethod));

        return toPaymentResponseWithUrl(paymentRepository.save(payment), httpRequest);
    }

    private PaymentResponse toPaymentResponseWithUrl(Payment payment, HttpServletRequest httpRequest)
    {
        expirePaymentIfNeeded(payment);
        PaymentResponse response = paymentMapper.toPaymentResponse(payment);
        if (payment.getPaymentMethod() == PaymentMethod.VNPAY
                && isPaymentWaiting(payment.getPaymentStatus())
                && !isExpired(payment.getPaymentExpiredAt()))
        {
            response.setPaymentUrl(createVNPayUrl(payment, httpRequest));
        }
        return response;
    }

    private PaymentResponse firstPaymentResponse(PaymentGroup paymentGroup)
    {
        return paymentRepository.findActiveByPaymentGroupId(paymentGroup.getPaymentGroupId())
                .stream()
                .findFirst()
                .map(paymentMapper::toPaymentResponse)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));
    }

    private void expirePaymentIfNeeded(Payment payment)
    {
        if (payment.getPaymentMethod() == PaymentMethod.VNPAY
                && isPaymentWaiting(payment.getPaymentStatus())
                && isExpired(payment.getPaymentExpiredAt()))
        {
            payment.setPaymentStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
        }
    }

    private PaymentStatus initialPaymentStatus(PaymentMethod paymentMethod)
    {
        return paymentMethod == PaymentMethod.VNPAY ? PaymentStatus.PROCESSING : PaymentStatus.PENDING;
    }

    private LocalDateTime expiresAtFor(PaymentMethod paymentMethod)
    {
        return paymentMethod == PaymentMethod.VNPAY
                ? LocalDateTime.now().plusMinutes(VNPAY_PAYMENT_TTL_MINUTES)
                : null;
    }

    private boolean isPaymentWaiting(PaymentStatus paymentStatus)
    {
        return paymentStatus == PaymentStatus.PENDING || paymentStatus == PaymentStatus.PROCESSING;
    }

    private boolean isFinalStatus(PaymentStatus paymentStatus)
    {
        return paymentStatus == PaymentStatus.PAID
                || paymentStatus == PaymentStatus.FAILED
                || paymentStatus == PaymentStatus.REFUNDED
                || paymentStatus == PaymentStatus.PARTIALLY_REFUNDED;
    }

    private boolean isExpired(LocalDateTime expiredAt)
    {
        return expiredAt != null && !expiredAt.isAfter(LocalDateTime.now());
    }

    private double getBookingAmount(Booking booking)
    {
        return safeDouble(booking.getTotalAmount() != null ? booking.getTotalAmount() : booking.getTotalPrice());
    }

    private String createVNPayUrl(Payment payment, HttpServletRequest request)
    {
        return createVNPayUrl(
                String.valueOf(payment.getBooking().getBookingId()),
                payment.getAmount(),
                "Thanh toan booking #" + payment.getBooking().getBookingId(),
                payment.getPaymentExpiredAt(),
                request
        );
    }

    private String createVNPayUrl(PaymentGroup paymentGroup, HttpServletRequest request)
    {
        return createVNPayUrl(
                paymentGroup.getPaymentGroupId(),
                paymentGroup.getAmount(),
                paymentGroup.getVnpayOrderInfo(),
                paymentGroup.getPaymentExpiredAt(),
                request
        );
    }

    private String createVNPayUrl(String txnRef, Double paymentAmount, String orderInfo, LocalDateTime expiredAt, HttpServletRequest request)
    {
        Map<String, String> vnpParams = new TreeMap<>();
        vnpParams.put("vnp_Version", "2.1.0");
        vnpParams.put("vnp_Command", "pay");
        vnpParams.put("vnp_TmnCode", vnPayConfig.getTmnCode());
        vnpParams.put("vnp_Amount", String.valueOf(Math.round(safeDouble(paymentAmount) * 100)));
        vnpParams.put("vnp_CurrCode", "VND");
        vnpParams.put("vnp_TxnRef", txnRef);
        vnpParams.put("vnp_OrderInfo", orderInfo);
        vnpParams.put("vnp_OrderType", "other");
        vnpParams.put("vnp_Locale", "vn");
        vnpParams.put("vnp_ReturnUrl", vnPayConfig.getReturnUrl());
        vnpParams.put("vnp_IpAddr", getClientIp(request));
        vnpParams.put("vnp_CreateDate", LocalDateTime.now().format(VNPAY_DATE_TIME_FORMATTER));
        if (expiredAt != null) {
            vnpParams.put("vnp_ExpireDate", expiredAt.format(VNPAY_DATE_TIME_FORMATTER));
        }

        String queryString = buildQueryString(vnpParams);
        String signature = createSignature(vnpParams);
        return vnPayConfig.getPaymentUrl() + "?" + queryString + "&vnp_SecureHash=" + signature;
    }

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

    private Long parseBookingTxnRef(String txnRef)
    {
        try
        {
            return Long.parseLong(txnRef);
        }
        catch (NumberFormatException exception)
        {
            throw new AppException(ErrorCode.PAYMENT_NOT_FOUND);
        }
    }

    private double safeDouble(Double value)
    {
        return value == null ? 0 : value;
    }
}
