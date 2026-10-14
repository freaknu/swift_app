package com.swiftapp_backend.paymentservice.payment.api.dto;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.swiftapp_backend.paymentservice.payment.domain.model.VerifyPaymentCommand;
import jakarta.validation.constraints.*;
public record VerifyPaymentRequest(
    @NotBlank @Size(max = 100) @JsonAlias("razorpay_order_id") String razorpayOrderId,
    @NotBlank @Size(max = 100) @JsonAlias("razorpay_payment_id") String razorpayPaymentId,
    @NotBlank @Pattern(regexp = "[0-9a-fA-F]{64}") @JsonAlias("razorpay_signature") String razorpaySignature) {
    public VerifyPaymentCommand command() { return new VerifyPaymentCommand(razorpayOrderId, razorpayPaymentId, razorpaySignature); }
}
