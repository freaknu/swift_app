package com.razorpay;

import com.swiftapp_backend.paymentservice.payment.domain.model.Payment;
import com.swiftapp_backend.paymentservice.payment.domain.model.ProviderOrder;
import com.swiftapp_backend.paymentservice.payment.domain.model.ProviderPayment;
import com.swiftapp_backend.paymentservice.payment.domain.exception.OrderCreationRejectedException;
import com.swiftapp_backend.paymentservice.payment.domain.exception.PaymentException;
import com.swiftapp_backend.paymentservice.payment.infrastructure.razorpay.adapter.RazorpayAdapter;
import com.swiftapp_backend.paymentservice.payment.infrastructure.razorpay.configs.RazorpaySignatureVerifier;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/** Real SDK entities and offline SDK clients exercise the production conversion path. */
class RazorpayAdapterEntityTest {
    private RazorpayAdapter adapter;
    private OfflineOrders orders;
    private OfflinePayments payments;

    @BeforeEach
    void setUp() throws RazorpayException {
        adapter = new RazorpayAdapter("rzp_test_example", "test_secret", "", new RazorpaySignatureVerifier());
        RazorpayClient client = new RazorpayClient("rzp_test_example", "test_secret");
        orders = new OfflineOrders();
        payments = new OfflinePayments();
        client.orders = orders;
        client.payments = payments;
        ReflectionTestUtils.setField(adapter, "client", client);
    }

    @Test
    void creatingAnOrderMapsSdkStringsAndIntegerAmount() {
        Payment ride = Payment.builder().rideId(10L).riderId(20L).amount(12345L)
            .currency("INR").receipt("ride_receipt").build();

        assertEquals(new ProviderOrder("order_test", 12345L, "INR", "ride_receipt"), adapter.createOrder(ride));
        assertEquals(12345L, orders.createRequest.getLong("amount"));
        assertEquals("10", orders.createRequest.getJSONObject("notes").getString("ride_id"));
    }

    @Test
    void receiptRecoveryFiltersSdkStringsAndPreservesLongAmounts() {
        orders.matches = List.of(order("order_unrelated", 12345, "different_receipt"),
            order("order_recovered", 4_000_000_000L, "ride_receipt"));

        assertEquals(new ProviderOrder("order_recovered", 4_000_000_000L, "INR", "ride_receipt"),
            adapter.findOrderByReceipt("ride_receipt"));
        assertEquals("ride_receipt", orders.fetchRequest.getString("receipt"));
    }

    @Test
    void recoveryUsesExactReceiptAndPaginatesContainsMatches() {
        orders.pages = Map.of(0, IntStream.range(0, 100).mapToObj(i -> order("order_other_" + i, 12345,
            "prefix_ride_receipt_" + i)).toList(), 100, List.of(order("order_recovered", 12345, "ride_receipt")));

        assertEquals("order_recovered", adapter.findOrderByReceipt("ride_receipt").orderId());
        assertEquals(100, orders.fetchRequest.getInt("skip"));
    }

    @Test
    void duplicateExactReceiptsRequireReconciliationInsteadOfChoosingAnOrder() {
        orders.matches = List.of(order("order_one", 12345, "ride_receipt"), order("order_two", 12345, "ride_receipt"));

        assertEquals(PaymentException.Kind.PROVIDER_FAILURE,
            assertThrows(PaymentException.class, () -> adapter.findOrderByReceipt("ride_receipt")).getKind());
    }

    @Test
    void confirmedRejectionIsDistinguishedFromAmbiguousProviderFailures() {
        Payment ride = Payment.builder().rideId(10L).riderId(20L).amount(12345L).currency("INR").receipt("ride_receipt").build();
        orders.createFailure = new RazorpayException("Offline rejection", 400);
        assertInstanceOf(OrderCreationRejectedException.class, assertThrows(PaymentException.class, () -> adapter.createOrder(ride)));
        for (int status : List.of(408, 409, 429, 500)) {
            orders.createFailure = new RazorpayException("Offline ambiguous response", status);
            assertFalse(assertThrows(PaymentException.class, () -> adapter.createOrder(ride)) instanceof OrderCreationRejectedException);
        }
        orders.createFailure = new RazorpayException("Offline response lost");
        assertFalse(assertThrows(PaymentException.class, () -> adapter.createOrder(ride)) instanceof OrderCreationRejectedException);
    }

    @Test
    void missingCredentialsCannotLeaveAnAmbiguousOrderAttempt() {
        RazorpayAdapter unconfigured = new RazorpayAdapter("", "", "", new RazorpaySignatureVerifier());
        var failure = assertThrows(OrderCreationRejectedException.class, () -> unconfigured.createOrder(Payment.builder().build()));
        assertEquals(PaymentException.Kind.UNAVAILABLE, failure.getKind());
    }

    @Test
    void fetchingPaymentMapsSdkStringFieldsAndIntegerAmount() {
        assertEquals(new ProviderPayment("pay_test", "order_test", 12345L, "INR", "authorized"),
            adapter.fetchPayment("pay_test"));
    }

    @Test
    void capturingPaymentMapsSdkFieldsAndSendsTheAuthoritativeAmount() {
        assertEquals(new ProviderPayment("pay_test", "order_test", 12345L, "INR", "captured"),
            adapter.capturePayment("pay_test", 12345L, "INR"));
        assertEquals(12345L, payments.captureRequest.getLong("amount"));
        assertEquals("INR", payments.captureRequest.getString("currency"));
    }

    private static Order order(String id, Number amount, String receipt) {
        return new Order(new JSONObject().put("id", id).put("amount", amount)
            .put("currency", "INR").put("receipt", receipt));
    }

    private static com.razorpay.Payment providerPayment(String status) {
        return new com.razorpay.Payment(new JSONObject().put("id", "pay_test").put("order_id", "order_test")
            .put("amount", 12345).put("currency", "INR").put("status", status));
    }

    // SDK client constructors are package-private; these substitutes never invoke HTTP.
    private static class OfflineOrders extends OrderClient {
        JSONObject createRequest;
        JSONObject fetchRequest;
        List<Order> matches = List.of();
        Map<Integer, List<Order>> pages = Map.of();
        RazorpayException createFailure;
        OfflineOrders() { super("offline"); }
        @Override public Order create(JSONObject request) throws RazorpayException {
            createRequest = request;
            if (createFailure != null) throw createFailure;
            return order("order_test", 12345, "ride_receipt");
        }
        @Override public List<Order> fetchAll(JSONObject request) {
            fetchRequest = request;
            return pages.getOrDefault(request.optInt("skip"), matches);
        }
    }

    private static class OfflinePayments extends PaymentClient {
        JSONObject captureRequest;
        OfflinePayments() { super("offline"); }
        @Override public com.razorpay.Payment fetch(String id) {
            assertEquals("pay_test", id);
            return providerPayment("authorized");
        }
        @Override public com.razorpay.Payment capture(String id, JSONObject request) {
            assertEquals("pay_test", id);
            captureRequest = request;
            return providerPayment("captured");
        }
    }
}
