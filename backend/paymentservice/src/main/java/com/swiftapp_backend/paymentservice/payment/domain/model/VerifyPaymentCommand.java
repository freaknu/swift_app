package com.swiftapp_backend.paymentservice.payment.domain.model;
public record VerifyPaymentCommand(String razorpayOrderId, String razorpayPaymentId, String razorpaySignature) {}
