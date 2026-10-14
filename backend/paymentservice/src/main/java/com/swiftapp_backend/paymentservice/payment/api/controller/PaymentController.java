package com.swiftapp_backend.paymentservice.payment.api.controller;
import com.swiftapp_backend.paymentservice.payment.api.dto.*;
import com.swiftapp_backend.paymentservice.payment.application.service.PaymentService;
import com.swiftapp_backend.paymentservice.shared.ApiResponseFormat;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/payments") @RequiredArgsConstructor @Validated
public class PaymentController {
    private final PaymentService payments;
    @GetMapping("/rides/{rideId}")
    public ApiResponseFormat<PaymentResponse> getPayment(@RequestHeader("x-user-id") @Positive Long authUserId,
            @PathVariable @Positive Long rideId, HttpServletRequest request) {
        return ApiResponseFormat.success(PaymentResponse.from(payments.getRidePayment(rideId, authUserId), payments.razorpayKeyId()), request.getRequestURI());
    }
    @PostMapping("/rides/{rideId}/order")
    public ApiResponseFormat<PaymentResponse> createOrder(@RequestHeader("x-user-id") @Positive Long authUserId,
            @PathVariable @Positive Long rideId, HttpServletRequest request) {
        return ApiResponseFormat.success(PaymentResponse.from(payments.createOrder(rideId, authUserId), payments.razorpayKeyId()), request.getRequestURI());
    }
    @PostMapping("/rides/{rideId}/verify")
    public ApiResponseFormat<PaymentResponse> verifyPayment(@RequestHeader("x-user-id") @Positive Long authUserId,
            @PathVariable @Positive Long rideId, @Valid @RequestBody VerifyPaymentRequest body, HttpServletRequest request) {
        return ApiResponseFormat.success(PaymentResponse.from(payments.verifyPayment(rideId, authUserId, body.command()), payments.razorpayKeyId()), request.getRequestURI());
    }
    @PostMapping(value = "/webhook", consumes = "application/json")
    public ApiResponseFormat<Void> webhook(@RequestHeader("X-Razorpay-Signature") String signature,
            @RequestBody byte[] body, HttpServletRequest request) {
        payments.handleWebhook(body, signature);
        return ApiResponseFormat.success(null, request.getRequestURI());
    }
}
