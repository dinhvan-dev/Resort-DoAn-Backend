package com.example.resort.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum ErrorCode {
    // Room errors
    ROOM_NOT_FOUND(1001, "Room not found", HttpStatus.NOT_FOUND),
    INVALID_ROOM_TYPE(1002, "Invalid room type", HttpStatus.BAD_REQUEST),
    INVALID_ROOM_STATUS(1003, "Invalid room status", HttpStatus.BAD_REQUEST),
    ROOM_ALREADY_EXISTS(1004, "Room already exists", HttpStatus.CONFLICT),
    INTERNAL_SERVER_ERROR(1005, "Internal server error", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_ROOM_NUMBER(1006, "Room number must contain floor followed by two room digits", HttpStatus.BAD_REQUEST),
    ROOM_UNDER_MAINTENANCE(1007, "Room is under maintenance", HttpStatus.CONFLICT),
    ROOM_NOT_AVAILABLE(1008, "Room is not ready for booking", HttpStatus.CONFLICT),
    INVALID_ROOM_IMAGES(1009, "Room images must be image files, contain between 3 and 5 images, and be no larger than 10MB each", HttpStatus.BAD_REQUEST),

    // Customer errors
    CUSTOMER_NOT_FOUND(2001, "Customer not found", HttpStatus.NOT_FOUND),
    CUSTOMER_EMAIL_EXISTS(2002, "Email already exists", HttpStatus.CONFLICT),
    CUSTOMER_PHONE_EXISTS(2003, "Phone number already exists", HttpStatus.CONFLICT),
    CUSTOMER_IDENTITY_EXISTS(2004, "Identity number already exists", HttpStatus.CONFLICT),
    CUSTOMER_PROFILE_EXISTS(2005, "Customer profile already exists for this user", HttpStatus.CONFLICT),
    CUSTOMER_ACCESS_DENIED(2006, "You can only access your own customer profile", HttpStatus.FORBIDDEN),

    // Booking errors
    BOOKING_NOT_FOUND(3001, "Booking not found", HttpStatus.NOT_FOUND),
    BOOKING_ROOM_UNAVAILABLE(3002, "Room is not available for the selected dates", HttpStatus.CONFLICT),
    BOOKING_INVALID_DATE(3003, "Check-in date must be before check-out date", HttpStatus.CONFLICT),
    BOOKING_CANNOT_CANCEL(3004, "Only PENDING or CONFIRMED Booking can be cancelled", HttpStatus.CONFLICT),
    BOOKING_ALREADY_CANCELLED(3005, "Booking has already been cancelled", HttpStatus.CONFLICT),
    BOOKING_INVALID_STATUS_TRANSITION(3006, "Invalid booking status transition", HttpStatus.CONFLICT),
    BOOKING_NOT_CONFIRMED(3007, "Booking not confirmed", HttpStatus.CONFLICT),

    USER_NOT_FOUND(5001, "User not found", HttpStatus.NOT_FOUND),
    USER_ALREADY_EXISTS(5002, "Username already exists", HttpStatus.CONFLICT),
    USER_NOT_EXISTS(5003, "Username does not exist", HttpStatus.NOT_FOUND),
    UNAUTHENTICATED(5004, "Unauthenticated", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(5005, "Access denied", HttpStatus.UNAUTHORIZED),
    USER_EMAIL_EXISTS(5006, "Email already exists", HttpStatus.CONFLICT),
    INVALID_USER_ROLE(5007, "Only USER, STAFF or HOUSEKEEPING roles can be assigned through this API", HttpStatus.BAD_REQUEST),
    USER_PHONE_EXISTS(5008, "Phone number already exists", HttpStatus.CONFLICT),
    INVALID_CURRENT_PASSWORD(5009, "Current password is incorrect", HttpStatus.BAD_REQUEST),

    // Validation
    INVALID_REQUEST(4000, "Invalid request", HttpStatus.BAD_REQUEST),
    INVALID_KEY(4001, "Uncategorized error", HttpStatus.BAD_REQUEST),

    // refreshToken
    REFRESH_TOKEN_NOT_FOUND(6000, "Refresh token not found", HttpStatus.NOT_FOUND),
    REFRESH_TOKEN_REVOKED(6001, "refresh token revoked", HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_EXPIRED(6002, "refresh token expired", HttpStatus.UNAUTHORIZED),

    // payment
    PAYMENT_NOT_FOUND(7001, "Payment not found", HttpStatus.NOT_FOUND),
    PAYMENT_ALREADY_EXISTS(7002, "Payment already exists for this booking", HttpStatus.CONFLICT),
    PAYMENT_ALREADY_PROCESSED(7003, "Payment has already been processed", HttpStatus.CONFLICT),
    PAYMENT_INVALID_SIGNATURE(7004, "Invalid payment signature", HttpStatus.BAD_REQUEST),
    PAYMENT_INVALID_METHOD(7005, "Invalid payment method for this operation", HttpStatus.BAD_REQUEST),
    INVALID_PAYMENT_METHOD(7007, "This operation is not allowed for this payment method", HttpStatus.BAD_REQUEST),
    PAYMENT_CANNOT_REFUND(7008, "Only paid payments can be refunded", HttpStatus.CONFLICT),

    // cleaning task
    CLEANING_TASK_NOT_FOUND(8001, "Cleaning task not found", HttpStatus.NOT_FOUND),
    CLEANING_TASK_ALREADY_EXISTS(8002, "Cleaning task already exists for this room", HttpStatus.CONFLICT),
    CLEANING_TASK_INVALID_STATUS(8003, "Invalid cleaning task status", HttpStatus.CONFLICT),
    ;

    private final int code;
    private final String message;
    private final HttpStatusCode statusCode;

    ErrorCode(int code, String message, HttpStatusCode statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }
}
