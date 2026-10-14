package com.swiftapp_backend.paymentservice.payment;

import com.swiftapp_backend.paymentservice.payment.domain.exception.PaymentException;
import com.swiftapp_backend.paymentservice.payment.infrastructure.razorpay.adapter.RazorpayAdapter;
import com.swiftapp_backend.paymentservice.payment.infrastructure.razorpay.configs.RazorpaySignatureVerifier;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class RazorpayOptionalWebhookTest {
    private static final String API_SECRET = "test_api_secret";
    private static final String WEBHOOK_SECRET = "separate_test_webhook_secret";

    @Test
    void checkoutSignatureUsesApiSecretWithoutConfiguredWebhookSecret() {
        RazorpayAdapter adapter = adapter(API_SECRET, "");
        byte[] signedCheckout = "order_test|pay_test".getBytes(StandardCharsets.UTF_8);
        assertTrue(adapter.verifyCheckoutSignature("order_test", "pay_test",
            PaymentFlowIntegrationTest.signature(signedCheckout, API_SECRET)));
        assertFalse(adapter.verifyCheckoutSignature("order_test", "pay_test",
            PaymentFlowIntegrationTest.signature(signedCheckout, WEBHOOK_SECRET)));
    }

    @Test
    void emptyWebhookSecretDoesNotAcceptAnUnsignedOrApiSignedWebhook() {
        RazorpayAdapter adapter = adapter(API_SECRET, "");
        byte[] body = "{\"event\":\"payment.captured\"}".getBytes(StandardCharsets.UTF_8);
        PaymentException unavailable = assertThrows(PaymentException.class,
            () -> adapter.verifyWebhookSignature(body, PaymentFlowIntegrationTest.signature(body, API_SECRET)));
        assertEquals(PaymentException.Kind.UNAVAILABLE, unavailable.getKind());
    }

    @Test
    void configuredWebhookUsesItsOwnSecretAndExactRawBody() {
        RazorpayAdapter adapter = adapter(API_SECRET, WEBHOOK_SECRET);
        byte[] body = "{\"event\":\"payment.captured\"}".getBytes(StandardCharsets.UTF_8);
        assertTrue(adapter.verifyWebhookSignature(body, PaymentFlowIntegrationTest.signature(body, WEBHOOK_SECRET)));
        assertFalse(adapter.verifyWebhookSignature(body, PaymentFlowIntegrationTest.signature(body, API_SECRET)));
        assertFalse(adapter.verifyWebhookSignature((new String(body, StandardCharsets.UTF_8) + " ").getBytes(StandardCharsets.UTF_8),
            PaymentFlowIntegrationTest.signature(body, WEBHOOK_SECRET)));
    }

    @Test
    void checkoutStillRequiresTheApiSecretInTestMode() {
        PaymentException unavailable = assertThrows(PaymentException.class,
            () -> adapter("", "").verifyCheckoutSignature("order_test", "pay_test", "0".repeat(64)));
        assertEquals(PaymentException.Kind.UNAVAILABLE, unavailable.getKind());
    }

    private RazorpayAdapter adapter(String apiSecret, String webhookSecret) {
        return new RazorpayAdapter("rzp_test_example", apiSecret, webhookSecret, new RazorpaySignatureVerifier());
    }
}
