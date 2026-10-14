package com.swiftapp_backend.paymentservice.payment.application.port;
import com.swiftapp_backend.paymentservice.payment.domain.event.PaymentStatusEvent;
public interface PaymentEventPort { void enqueue(String topic, PaymentStatusEvent event); }
