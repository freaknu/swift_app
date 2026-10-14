package com.swiftapp_backend.paymentservice.payment.infrastructure.razorpay.adapter;
import com.razorpay.*;
import com.swiftapp_backend.paymentservice.payment.application.port.RazorpayPort;
import com.swiftapp_backend.paymentservice.payment.domain.exception.PaymentException;
import com.swiftapp_backend.paymentservice.payment.domain.exception.OrderCreationRejectedException;
import com.swiftapp_backend.paymentservice.payment.domain.model.ProviderOrder;
import com.swiftapp_backend.paymentservice.payment.domain.model.ProviderPayment;
import com.swiftapp_backend.paymentservice.payment.domain.model.Payment;
import com.swiftapp_backend.paymentservice.payment.infrastructure.razorpay.configs.RazorpaySignatureVerifier;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
@Component
public class RazorpayAdapter implements RazorpayPort {
    private final String keyId;
    private final String keySecret;
    private final String webhookSecret;
    private final RazorpaySignatureVerifier signatures;
    private volatile RazorpayClient client;
    public RazorpayAdapter(@Value("${razorpay.key-id:}") String keyId,
                          @Value("${razorpay.key-secret:}") String keySecret,
                          @Value("${razorpay.webhook-secret:}") String webhookSecret,
                          RazorpaySignatureVerifier signatures) {
        this.keyId = keyId; this.keySecret = keySecret; this.webhookSecret = webhookSecret; this.signatures = signatures;
    }
    @Override public String keyId() { return keyId; }
    @Override public ProviderOrder findOrderByReceipt(String receipt) {
        try {
            ProviderOrder found = null;
            // Razorpay's receipt filter is a contains search, so inspect exact matches and paginate.
            for (int skip = 0; skip < 1000; skip += 100) {
                var page = client().orders.fetchAll(new JSONObject().put("receipt", receipt).put("count", 100).put("skip", skip));
                for (Order candidate : page) {
                    if (!receipt.equals(field(candidate, "receipt"))) continue;
                    ProviderOrder match = order(candidate);
                    if (found != null && !found.orderId().equals(match.orderId()))
                        throw new PaymentException(PaymentException.Kind.PROVIDER_FAILURE, "Multiple Razorpay orders share this receipt; reconciliation is required");
                    found = match;
                }
                if (page.size() < 100) return found;
            }
            throw new PaymentException(PaymentException.Kind.UNAVAILABLE, "Razorpay receipt lookup is incomplete; please retry later");
        } catch (RazorpayException ex) { throw providerFailure(); }
    }
    @Override public ProviderOrder createOrder(Payment payment) {
        if (keyId.isBlank() || keySecret.isBlank())
            throw new OrderCreationRejectedException(PaymentException.Kind.UNAVAILABLE, "Razorpay API credentials are not configured");
        try {
            JSONObject request = new JSONObject().put("amount", payment.getAmount()).put("currency", payment.getCurrency())
                .put("receipt", payment.getReceipt()).put("partial_payment", false)
                .put("notes", new JSONObject().put("ride_id", payment.getRideId().toString())
                    .put("rider_id", payment.getRiderId().toString()));
            return order(client().orders.create(request));
        } catch (RazorpayException ex) {
            int status = ex.getStatusCode();
            if (status >= 400 && status < 500 && status != 408 && status != 409 && status != 429)
                throw new OrderCreationRejectedException(PaymentException.Kind.PROVIDER_FAILURE, "Razorpay rejected the order; please retry");
            throw providerFailure();
        }
    }
    @Override public ProviderPayment fetchPayment(String id) {
        try { return payment(client().payments.fetch(id)); }
        catch (RazorpayException ex) { throw providerFailure(); }
    }
    @Override public ProviderPayment capturePayment(String id, long amount, String currency) {
        try { return payment(client().payments.capture(id, new JSONObject().put("amount", amount).put("currency", currency))); }
        catch (RazorpayException ex) {
            // Automatic capture or another webhook may have won the race. Fetch the authoritative state.
            return fetchPayment(id);
        }
    }
    @Override
    public boolean verifyCheckoutSignature(String orderId, String paymentId, String signature) {
        requireCredentials();
        return signatures.verify((orderId + "|" + paymentId).getBytes(StandardCharsets.UTF_8), signature, keySecret);
    }
    @Override
    public boolean verifyWebhookSignature(byte[] body, String signature) {
        if (webhookSecret.isBlank()) throw new PaymentException(PaymentException.Kind.UNAVAILABLE, "Razorpay webhook secret is not configured");
        return signatures.verify(body, signature, webhookSecret);
    }
    private synchronized RazorpayClient client() throws RazorpayException {
        requireCredentials();
        if (client == null) client = new RazorpayClient(keyId, keySecret);
        return client;
    }
    private void requireCredentials() {
        if (keyId.isBlank() || keySecret.isBlank()) throw new PaymentException(PaymentException.Kind.UNAVAILABLE, "Razorpay API credentials are not configured");
    }
    private PaymentException providerFailure() { return new PaymentException(PaymentException.Kind.PROVIDER_FAILURE, "Razorpay request failed; please retry"); }
    private ProviderOrder order(Order o) {
        return new ProviderOrder(field(o, "id"), Long.parseLong(field(o, "amount")),
            field(o, "currency"), field(o, "receipt"));
    }
    private ProviderPayment payment(com.razorpay.Payment p) {
        return new ProviderPayment(field(p, "id"), field(p, "order_id"),
            Long.parseLong(field(p, "amount")), field(p, "currency"), field(p, "status"));
    }
    private String field(Entity entity, String name) {
        // Entity.get is generic. Passing it directly to String.valueOf selects the char[] overload.
        Object value = entity.get(name);
        return value == null ? null : String.valueOf(value);
    }
}
