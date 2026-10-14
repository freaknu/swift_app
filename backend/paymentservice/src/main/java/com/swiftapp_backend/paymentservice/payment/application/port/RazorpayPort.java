package com.swiftapp_backend.paymentservice.payment.application.port;
import com.swiftapp_backend.paymentservice.payment.domain.model.*;
public interface RazorpayPort {
    ProviderOrder createOrder(Payment payment);
    ProviderOrder findOrderByReceipt(String receipt);
    ProviderPayment fetchPayment(String paymentId);
    ProviderPayment capturePayment(String paymentId, long amount, String currency);
    boolean verifyCheckoutSignature(String orderId, String paymentId, String signature);
    boolean verifyWebhookSignature(byte[] body, String signature);
    String keyId();
}
