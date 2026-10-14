package com.swiftapp_backend.paymentservice.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftapp_backend.paymentservice.payment.application.port.*;
import com.swiftapp_backend.paymentservice.payment.application.service.PaymentService;
import com.swiftapp_backend.paymentservice.payment.domain.enums.PaymentStatus;
import com.swiftapp_backend.paymentservice.payment.domain.event.RideCompletedEvent;
import com.swiftapp_backend.paymentservice.payment.domain.exception.PaymentException;
import com.swiftapp_backend.paymentservice.payment.domain.model.*;
import com.swiftapp_backend.paymentservice.payment.domain.repository.PaymentRepository;
import com.swiftapp_backend.paymentservice.payment.infrastructure.persistence.adapter.JpaPaymentRepository;
import com.swiftapp_backend.paymentservice.payment.infrastructure.persistence.repository.*;
import com.swiftapp_backend.paymentservice.payment.infrastructure.razorpay.configs.RazorpaySignatureVerifier;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.*;
import org.springframework.context.annotation.*;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:payments;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop", "eureka.client.enabled=false", "spring.cloud.discovery.enabled=false",
    "payment.consumer.enabled=false", "payment.outbox.enabled=false", "razorpay.webhook-secret="
})
@AutoConfigureMockMvc
@Import(PaymentFlowIntegrationTest.Fakes.class)
class PaymentFlowIntegrationTest {
    @Autowired PaymentService service;
    @Autowired MockMvc mvc;
    @Autowired FakeRazorpay provider;
    @Autowired FakeRides rides;
    @Autowired PaymentJpaRepository payments;
    @Autowired PaymentOrderJpaRepository orders;
    @Autowired PaymentOutboxJpaRepository outbox;
    @Autowired RacingRepository racing;
    @Autowired ObjectMapper mapper;
    @Autowired PaymentUnitOfWork unitOfWork;
    @Autowired org.springframework.transaction.PlatformTransactionManager transactionManager;

    @BeforeEach void setup() {
        outbox.deleteAll(); orders.deleteAll(); payments.deleteAll(); provider.reset(); rides.reset(); racing.barrier = null;
    }
    private RideCompletedEvent event() { return new RideCompletedEvent(10L, 20L, 30L, new BigDecimal("123.45"), "INR", "2026-10-13T10:00:00Z"); }
    private Payment create() { service.handleRideComplete(event()); return service.getRidePayment(10L, 100L); }
    private VerifyPaymentCommand verification(Payment p, String paymentId) {
        return new VerifyPaymentCommand(p.getRazorpayOrderId(), paymentId, signature((p.getRazorpayOrderId() + "|" + paymentId).getBytes(StandardCharsets.UTF_8), FakeRazorpay.SECRET));
    }
    private byte[] webhook(String type, Payment p, String paymentId) {
        return ("{\"event\":\"" + type + "\",\"payload\":{\"payment\":{\"entity\":{\"id\":\"" + paymentId + "\",\"order_id\":\"" + p.getRazorpayOrderId() + "\"}}}}").getBytes(StandardCharsets.UTF_8);
    }
    private void sendWebhook(String type, Payment p, String paymentId) {
        byte[] body = webhook(type, p, paymentId);
        service.handleWebhook(body, signature(body, FakeRazorpay.WEBHOOK_SECRET));
    }

    @Test void completedRideCreatesOnePersistentOrderWithMinorUnitsAndOutbox() throws Exception {
        Payment p = create(); service.handleRideComplete(event()); service.createOrder(10L, 100L);
        assertEquals(PaymentStatus.ORDER_CREATED, p.getStatus()); assertEquals(12345, p.getAmount());
        assertEquals(1, provider.creates.get()); assertEquals(1, payments.count()); assertEquals(1, orders.count()); assertEquals(1, outbox.count());
        var row = outbox.findAll().getFirst(); assertEquals("payment.order.created", row.getTopic());
        var payload = mapper.readTree(row.getPayload()); assertEquals("ORDER_CREATED", payload.path("paymentStatus").asText());
        assertEquals(12345, payload.path("amount").asLong()); assertTrue(payload.has("eventTime")); assertFalse(payload.has("razorpaySignature"));
    }
    @Test void signedCheckoutCapturesAuthorizedFundsAndEnqueuesSuccessExactlyOnce() {
        Payment p = create(); provider.setPayment(p, "pay_one", "authorized");
        assertEquals(PaymentStatus.COMPLETED, service.verifyPayment(10L, 100L, verification(p, "pay_one")).getStatus());
        service.verifyPayment(10L, 100L, verification(p, "pay_one")); sendWebhook("payment.captured", p, "pay_one");
        assertEquals(1, provider.captures.get()); assertEquals(2, outbox.count());
        assertEquals(1, outbox.findAll().stream().filter(r -> "payment.completed".equals(r.getTopic())).count());
    }
    @Test void checkoutCallbackWithoutCapturedFundsCannotReportSuccess() {
        Payment p = create(); provider.setPayment(p, "pay_created", "created");
        assertEquals(PaymentStatus.ORDER_CREATED, service.verifyPayment(10L, 100L, verification(p, "pay_created")).getStatus());
        assertEquals(1, outbox.count());
    }
    @Test void captureStillPendingCannotReportSuccess() {
        Payment p = create(); provider.capturePending = true; provider.setPayment(p, "pay_authorized", "authorized");
        assertEquals(PaymentStatus.ORDER_CREATED, service.verifyPayment(10L, 100L, verification(p, "pay_authorized")).getStatus());
        assertEquals(1, outbox.count());
    }
    @Test void badCheckoutSignatureDoesNotFetchOrCompletePayment() {
        Payment p = create(); provider.setPayment(p, "pay_forged", "captured");
        assertThrows(PaymentException.class, () -> service.verifyPayment(10L, 100L, new VerifyPaymentCommand(p.getRazorpayOrderId(), "pay_forged", "0".repeat(64))));
        assertEquals(0, provider.fetches.get()); assertEquals(PaymentStatus.ORDER_CREATED, payments.findById(10L).orElseThrow().getStatus());
    }
    @Test void providerAmountCurrencyOrderAndPaymentIdAreAllBoundToStoredRide() {
        Payment p = create();
        for (ProviderPayment invalid : List.of(
            new ProviderPayment("pay_wrong", p.getRazorpayOrderId(), p.getAmount() + 1, "INR", "captured"),
            new ProviderPayment("pay_wrong", p.getRazorpayOrderId(), p.getAmount(), "USD", "captured"),
            new ProviderPayment("pay_wrong", "other_order", p.getAmount(), "INR", "captured"),
            new ProviderPayment("different_payment", p.getRazorpayOrderId(), p.getAmount(), "INR", "captured"))) {
            provider.providerPayments.put("pay_wrong", invalid);
            assertThrows(PaymentException.class, () -> service.verifyPayment(10L, 100L, verification(p, "pay_wrong")));
        }
        assertEquals(PaymentStatus.ORDER_CREATED, payments.findById(10L).orElseThrow().getStatus()); assertEquals(1, outbox.count());
    }
    @Test void driverMayReadParticipantPaymentButCannotPay() {
        create(); assertEquals(PaymentStatus.ORDER_CREATED, service.getRidePayment(10L, 200L).getStatus());
        assertEquals(PaymentException.Kind.FORBIDDEN, assertThrows(PaymentException.class, () -> service.createOrder(10L, 200L)).getKind());
        assertEquals(PaymentException.Kind.FORBIDDEN, assertThrows(PaymentException.class, () -> service.getRidePayment(10L, 300L)).getKind());
    }
    @Test void activeRideCannotInitiatePayment() {
        rides.completed = false;
        assertEquals(PaymentException.Kind.CONFLICT, assertThrows(PaymentException.class, () -> service.createOrder(10L, 100L)).getKind());
        assertEquals(0, payments.count());
    }
    @Test void restRecoveryCreatesOrderFromTrustedFareBeforeKafkaDelivery() {
        Payment p = service.createOrder(10L, 100L); service.handleRideComplete(event());
        assertEquals(12345, p.getAmount()); assertEquals(1, provider.creates.get());
    }
    @Test void providerCrashAfterOrderCreationRecoversByDurableReceipt() {
        provider.crashAfterCreate = true;
        assertThrows(PaymentException.class, () -> service.handleRideComplete(event()));
        assertEquals(PaymentStatus.PENDING, payments.findById(10L).orElseThrow().getStatus());
        String receipt = payments.findById(10L).orElseThrow().getReceipt();
        provider.crashAfterCreate = false; service.handleRideComplete(event());
        assertEquals(receipt, payments.findById(10L).orElseThrow().getReceipt()); assertEquals(1, provider.creates.get()); assertEquals(1, outbox.count());
    }
    @Test void duplicateOrConflictingRideEventsCannotOverwriteExistingPayment() {
        Payment p = create();
        assertThrows(PaymentException.class, () -> service.handleRideComplete(new RideCompletedEvent(10L, 999L, 30L, event().totalFare(), "INR", event().completedAt())));
        assertEquals(p.getRazorpayOrderId(), payments.findById(10L).orElseThrow().getRazorpayOrderId()); assertEquals(20L, payments.findById(10L).orElseThrow().getRiderId());
    }
    @Test void invalidFareOrCurrencyAreRejectedBeforePersistence() {
        for (BigDecimal fare : List.of(new BigDecimal("0.5"), new BigDecimal("-1"), new BigDecimal("1.001")))
            assertThrows(PaymentException.class, () -> service.handleRideComplete(new RideCompletedEvent(10L, 20L, 30L, fare, "INR", event().completedAt())));
        for (String currency : Arrays.asList("USD", "", null))
            assertThrows(PaymentException.class, () -> service.handleRideComplete(new RideCompletedEvent(10L, 20L, 30L, event().totalFare(), currency, event().completedAt())));
        assertEquals(0, payments.count());
    }
    @Test void webhookCompletesPaymentAndDuplicateOrLateFailuresCannotRegressIt() {
        Payment p = create(); provider.setPayment(p, "pay_webhook", "captured");
        sendWebhook("payment.captured", p, "pay_webhook"); sendWebhook("payment.captured", p, "pay_webhook");
        provider.setPayment(p, "pay_webhook", "failed"); sendWebhook("payment.failed", p, "pay_webhook");
        assertEquals(PaymentStatus.COMPLETED, payments.findById(10L).orElseThrow().getStatus()); assertEquals(2, outbox.count());
    }
    @Test void failedAttemptRetriesReuseOrderAndLaterCaptureCompletesOnce() {
        Payment p = create(); provider.setPayment(p, "pay_failed", "failed"); sendWebhook("payment.failed", p, "pay_failed");
        assertEquals(PaymentStatus.FAILED, payments.findById(10L).orElseThrow().getStatus());
        Payment retry = service.createOrder(10L, 100L); assertEquals(p.getRazorpayOrderId(), retry.getRazorpayOrderId());
        assertEquals(PaymentStatus.ORDER_CREATED, retry.getStatus()); assertEquals(1, provider.creates.get());
        provider.setPayment(p, "pay_failed", "captured"); sendWebhook("payment.captured", p, "pay_failed");
        assertEquals(PaymentStatus.COMPLETED, service.createOrder(10L, 100L).getStatus()); assertEquals(1, provider.creates.get());
    }
    @Test void webhookHmacUsesExactRawBytes() {
        Payment p = create(); provider.setPayment(p, "pay_raw", "captured"); byte[] body = webhook("payment.captured", p, "pay_raw");
        byte[] changed = (new String(body, StandardCharsets.UTF_8) + " ").getBytes(StandardCharsets.UTF_8);
        assertThrows(PaymentException.class, () -> service.handleWebhook(changed, signature(body, FakeRazorpay.WEBHOOK_SECRET)));
        assertEquals(PaymentStatus.ORDER_CREATED, payments.findById(10L).orElseThrow().getStatus());
    }
    @Test void concurrentFirstEventsNeverReplaceWinningReceiptOrCreateTwoOrders() throws Exception {
        racing.barrier = new CyclicBarrier(2);
        try (var workers = Executors.newFixedThreadPool(2)) {
            Future<?> a = workers.submit(() -> service.handleRideComplete(event()));
            Future<?> b = workers.submit(() -> service.handleRideComplete(event()));
            a.get(15, TimeUnit.SECONDS); b.get(15, TimeUnit.SECONDS);
        }
        assertEquals(1, payments.count()); assertEquals(1, provider.creates.get()); assertEquals(1, orders.count()); assertEquals(1, outbox.count());
        var p = payments.findById(10L).orElseThrow(); assertEquals(PaymentStatus.ORDER_CREATED, p.getStatus());
        assertTrue(provider.providerOrders.containsKey(p.getReceipt()));
    }
    @Test void concurrentVerificationCapturesOnceAndEnqueuesOneCompletion() throws Exception {
        Payment p = create(); provider.setPayment(p, "pay_race", "authorized");
        try (var workers = Executors.newFixedThreadPool(2)) {
            Future<?> a = workers.submit(() -> service.verifyPayment(10L, 100L, verification(p, "pay_race")));
            Future<?> b = workers.submit(() -> service.verifyPayment(10L, 100L, verification(p, "pay_race")));
            a.get(15, TimeUnit.SECONDS); b.get(15, TimeUnit.SECONDS);
        }
        assertEquals(1, provider.captures.get()); assertEquals(2, outbox.count());
    }
    @Test void localCheckoutWithoutWebhookSecretVerifiesThroughApiAndEnqueuesSuccess() throws Exception {
        provider.webhookSecret = "";
        Payment payment = service.createOrder(10L, 100L);
        provider.setPayment(payment, "pay_local_test", "authorized");
        VerifyPaymentCommand signed = verification(payment, "pay_local_test");
        mvc.perform(post("/api/v1/payments/rides/10/verify").header("x-user-id", "100")
                .contentType("application/json").content(mapper.writeValueAsString(signed)))
            .andExpect(status().isOk()).andExpect(jsonPath("success").value(true))
            .andExpect(jsonPath("data.status").value("COMPLETED"))
            .andExpect(jsonPath("data.razorpayPaymentId").value("pay_local_test"));
        assertEquals(1, provider.captures.get());
        assertEquals(1, outbox.findAll().stream().filter(row -> "payment.completed".equals(row.getTopic())).count());
        mvc.perform(post("/api/v1/payments/webhook").contentType("application/json")
                .header("X-Razorpay-Signature", "0".repeat(64)).content("{}"))
            .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("success").value(false));
    }

    @Test void apiReturnsCheckoutContractAndRequiresIdentity() throws Exception {
        mvc.perform(post("/api/v1/payments/rides/10/order").header("x-user-id", "100"))
            .andExpect(status().isOk()).andExpect(jsonPath("success").value(true))
            .andExpect(jsonPath("data.status").value("ORDER_CREATED")).andExpect(jsonPath("data.amount").value(12345))
            .andExpect(jsonPath("data.razorpayKeyId").value("rzp_test_example"));
        mvc.perform(get("/api/v1/payments/rides/10")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/payments/rides/10/order").header("x-user-id", "200")).andExpect(status().isForbidden());
    }
    @Test void apiRejectsForgedVerificationAndUnsignedWebhook() throws Exception {
        create();
        mvc.perform(post("/api/v1/payments/rides/10/verify").header("x-user-id", "100").contentType("application/json")
            .content("{\"razorpayOrderId\":\"order_1\",\"razorpayPaymentId\":\"pay_1\",\"razorpaySignature\":\"" + "0".repeat(64) + "\"}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("success").value(false));
        mvc.perform(post("/api/v1/payments/webhook").contentType("application/json").content("{}"))
            .andExpect(status().isBadRequest());
    }

    @Test void outboxKeepsFailedPublishQueuedAndRetriesSuccessfully() {
        create();
        var publisher = new RecordingKafka();
        var dispatcher = new com.swiftapp_backend.paymentservice.payment.infrastructure.kafka.service.PaymentOutboxDispatcher(outbox, publisher, transactionManager);
        publisher.fail = true;
        dispatcher.publishPending();
        assertNull(outbox.findAll().getFirst().getPublishedAt());
        publisher.fail = false;
        // Backoff is durable; make this single fixture due without sleeping.
        var retry = outbox.findAll().getFirst();
        retry.setNextAttemptAt(java.time.Instant.EPOCH);
        outbox.save(retry);
        dispatcher.publishPending();
        assertNotNull(outbox.findAll().getFirst().getPublishedAt());
        dispatcher.publishPending();
        assertEquals(2, publisher.sends); assertEquals("payment.order.created", publisher.topic); assertEquals("10", publisher.key);
        publisher.destroy();
    }
    @Test void canonicalKafkaCompletionIsParsedAndFailuresAreNotSwallowed() throws Exception {
        var consumer = new com.swiftapp_backend.paymentservice.payment.infrastructure.kafka.service.KafkaConsumerService(mapper, service);
        consumer.consume(mapper.writeValueAsString(event()));
        assertEquals(PaymentStatus.ORDER_CREATED, payments.findById(10L).orElseThrow().getStatus());
        assertThrows(java.io.IOException.class, () -> consumer.consume("not-json"));
        assertThrows(PaymentException.class, () -> consumer.consume("{\"rideId\":11,\"riderId\":20,\"driverId\":30}"));
        assertEquals(1, payments.count());
    }
    @Test void unsignedOrMalformedSignedWebhookCannotChangeState() {
        Payment p = create(); provider.setPayment(p, "pay_one", "captured");
        byte[] body = webhook("payment.captured", p, "pay_one");
        assertThrows(PaymentException.class, () -> service.handleWebhook(body, "0".repeat(64)));
        byte[] invalid = "{".getBytes(StandardCharsets.UTF_8);
        assertThrows(PaymentException.class, () -> service.handleWebhook(invalid, signature(invalid, FakeRazorpay.WEBHOOK_SECRET)));
        assertEquals(PaymentStatus.ORDER_CREATED, payments.findById(10L).orElseThrow().getStatus());
    }
    static class RecordingKafka extends org.springframework.kafka.core.KafkaTemplate<String, String> {
        boolean fail; int sends; String topic, key;
        RecordingKafka() { super(new org.springframework.kafka.core.DefaultKafkaProducerFactory<>(Map.of(
            "bootstrap.servers", "localhost:9092", "key.serializer", org.apache.kafka.common.serialization.StringSerializer.class,
            "value.serializer", org.apache.kafka.common.serialization.StringSerializer.class))); }
        @Override public CompletableFuture<org.springframework.kafka.support.SendResult<String, String>> send(String topic, String key, String payload) {
            this.topic = topic; this.key = key; sends++;
            return fail ? CompletableFuture.failedFuture(new IllegalStateException("Broker unavailable")) : CompletableFuture.completedFuture(null);
        }
    }

    static String signature(byte[] body, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256"); mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(body));
        } catch (Exception ex) { throw new IllegalStateException(ex); }
    }
    @TestConfiguration static class Fakes {
        @Bean @Primary FakeRazorpay fakeRazorpay() { return new FakeRazorpay(); }
        @Bean @Primary FakeRides fakeRides() { return new FakeRides(); }
        @Bean @Primary RacingRepository racingRepository(JpaPaymentRepository repository) { return new RacingRepository(repository); }
    }
    static class FakeRazorpay implements RazorpayPort {
        static final String SECRET = "test_api_secret", WEBHOOK_SECRET = "test_webhook_secret";
        final Map<String, ProviderOrder> providerOrders = new ConcurrentHashMap<>();
        final Map<String, ProviderPayment> providerPayments = new ConcurrentHashMap<>();
        final AtomicInteger creates = new AtomicInteger(), captures = new AtomicInteger(), fetches = new AtomicInteger();
        final RazorpaySignatureVerifier verifier = new RazorpaySignatureVerifier();
        volatile boolean crashAfterCreate, capturePending;
        volatile Runnable findHook, createHook, fetchHook, captureHook;
        volatile PaymentException createFailure;
        String webhookSecret = WEBHOOK_SECRET;
        void reset() { providerOrders.clear(); providerPayments.clear(); creates.set(0); captures.set(0); fetches.set(0); crashAfterCreate = false; capturePending = false; webhookSecret = WEBHOOK_SECRET;
            findHook = createHook = fetchHook = captureHook = null; createFailure = null;
        }
        private void outsideTransaction() {
            assertFalse(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive(),
                "Razorpay network calls must execute outside a database transaction");
        }
        private void run(Runnable hook) { if (hook != null) hook.run(); }
        void setPayment(Payment p, String id, String status) { providerPayments.put(id, new ProviderPayment(id, p.getRazorpayOrderId(), p.getAmount(), p.getCurrency(), status)); }
        @Override public ProviderOrder createOrder(Payment p) {
            outsideTransaction();
            if (createFailure != null) throw createFailure;
            var order = new ProviderOrder("order_" + creates.incrementAndGet(), p.getAmount(), p.getCurrency(), p.getReceipt());
            providerOrders.put(p.getReceipt(), order);
            run(createHook);
            if (crashAfterCreate) throw new PaymentException(PaymentException.Kind.PROVIDER_FAILURE, "Simulated interrupted provider response");
            return order;
        }
        @Override public ProviderOrder findOrderByReceipt(String receipt) {
            outsideTransaction(); run(findHook); return providerOrders.get(receipt);
        }
        @Override public ProviderPayment fetchPayment(String id) {
            outsideTransaction(); fetches.incrementAndGet();
            ProviderPayment snapshot = Objects.requireNonNull(providerPayments.get(id));
            run(fetchHook); return snapshot;
        }
        @Override public ProviderPayment capturePayment(String id, long amount, String currency) {
            outsideTransaction();
            captures.incrementAndGet(); var p = providerPayments.get(id);
            var captured = new ProviderPayment(id, p.orderId(), amount, currency, capturePending ? "authorized" : "captured");
            providerPayments.put(id, captured); run(captureHook); return captured;
        }
        @Override public boolean verifyCheckoutSignature(String orderId, String id, String signature) {
            return signingAdapter().verifyCheckoutSignature(orderId, id, signature);
        }
        @Override public boolean verifyWebhookSignature(byte[] body, String signature) {
            return signingAdapter().verifyWebhookSignature(body, signature);
        }
        private com.swiftapp_backend.paymentservice.payment.infrastructure.razorpay.adapter.RazorpayAdapter signingAdapter() {
            return new com.swiftapp_backend.paymentservice.payment.infrastructure.razorpay.adapter.RazorpayAdapter(
                keyId(), SECRET, webhookSecret, verifier);
        }
        @Override public String keyId() { return "rzp_test_example"; }
    }
    static class FakeRides implements RideLookupPort {
        boolean completed;
        void reset() { completed = true; }
        @Override public CompletedRide getParticipantRide(Long id, Long authId) {
            if (!Set.of(100L, 200L).contains(authId)) throw new PaymentException(PaymentException.Kind.FORBIDDEN, "Not a participant");
            return new CompletedRide(id, 20L, 30L, completed, new BigDecimal("123.45"), "INR", "2026-10-13T10:00:00Z");
        }
        @Override public Long riderIdForAuthUser(Long authId) { return authId == 100L ? 20L : 999L; }
    }
    static class RacingRepository implements PaymentRepository {
        private final PaymentRepository delegate;
        volatile CyclicBarrier barrier;
        RacingRepository(PaymentRepository delegate) { this.delegate = delegate; }
        @Override public Payment getByRideId(Long id) { return delegate.getByRideId(id); }
        @Override public Payment getLockedByRideId(Long id) {
            Payment p = delegate.getLockedByRideId(id); CyclicBarrier gate = barrier;
            if (p == null && gate != null) {
                try { gate.await(10, TimeUnit.SECONDS); } catch (Exception ex) { throw new IllegalStateException(ex); }
            }
            return p;
        }
        @Override public Payment insert(Payment p) { return delegate.insert(p); }
        @Override public Payment save(Payment p) { return delegate.save(p); }
        @Override public void recordOrder(Payment p) { delegate.recordOrder(p); }
        @Override public Long rideIdForOrder(String id) { return delegate.rideIdForOrder(id); }
        @Override public boolean orderBelongsToRide(String id, Long rideId) { return delegate.orderBelongsToRide(id, rideId); }
    }
}
