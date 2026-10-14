package com.swiftapp_backend.paymentservice.payment.domain.model;
public record ProviderOrder(String orderId, long amount, String currency, String receipt) {}
