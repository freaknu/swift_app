package com.swiftapp_backend.paymentservice.payment.domain.model;
public record ProviderPayment(String paymentId, String orderId, long amount, String currency, String status) {}
