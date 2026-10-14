package com.swiftapp_backend.paymentservice.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftapp_backend.paymentservice.payment.application.port.PaymentEventPort;
import com.swiftapp_backend.paymentservice.payment.application.port.PaymentUnitOfWork;
import com.swiftapp_backend.paymentservice.payment.application.service.PaymentService;
import com.swiftapp_backend.paymentservice.payment.domain.enums.PaymentStatus;
import com.swiftapp_backend.paymentservice.payment.domain.event.RideCompletedEvent;
import com.swiftapp_backend.paymentservice.payment.domain.exception.OrderCreationRejectedException;
import com.swiftapp_backend.paymentservice.payment.domain.exception.PaymentException;
import com.swiftapp_backend.paymentservice.payment.domain.model.Payment;
import com.swiftapp_backend.paymentservice.payment.domain.model.ProviderOrder;
import com.swiftapp_backend.paymentservice.payment.domain.model.VerifyPaymentCommand;
import com.swiftapp_backend.paymentservice.payment.domain.repository.PaymentRepository;
import com.swiftapp_backend.paymentservice.payment.infrastructure.persistence.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import javax.sql.DataSource;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:provider-concurrency;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop", "eureka.client.enabled=false", "spring.cloud.discovery.enabled=false",
    "payment.consumer.enabled=false", "payment.outbox.enabled=false", "razorpay.webhook-secret="
})
@Import(PaymentFlowIntegrationTest.Fakes.class)
class PaymentProviderConcurrencyTest {
    @Autowired PaymentService service;
    @Autowired PaymentUnitOfWork work;
    @Autowired PaymentRepository repository;
    @Autowired PaymentEventPort events;
    @Autowired ObjectMapper mapper;
    @Autowired PlatformTransactionManager manager;
    @Autowired DataSource dataSource;
    @Autowired PaymentFlowIntegrationTest.FakeRazorpay provider;
    @Autowired PaymentFlowIntegrationTest.FakeRides rides;
    @Autowired PaymentJpaRepository payments;
    @Autowired PaymentOrderJpaRepository orders;
    @Autowired PaymentOutboxJpaRepository outbox;

    @BeforeEach void reset() {
        outbox.deleteAll(); orders.deleteAll(); payments.deleteAll(); provider.reset(); rides.reset();
    }

    private RideCompletedEvent event() {
        return new RideCompletedEvent(10L, 20L, 30L, new BigDecimal("123.45"), "INR", "2026-10-13T10:00:00Z");
    }
    private Payment create() { service.handleRideComplete(event()); return service.getRidePayment(10L, 100L); }
    private VerifyPaymentCommand verification(Payment p, String paymentId) {
        return new VerifyPaymentCommand(p.getRazorpayOrderId(), paymentId, PaymentFlowIntegrationTest.signature(
            (p.getRazorpayOrderId() + "|" + paymentId).getBytes(StandardCharsets.UTF_8), PaymentFlowIntegrationTest.FakeRazorpay.SECRET));
    }
    private void webhook(Payment p, String paymentId) {
        byte[] body = ("{\"event\":\"payment.captured\",\"payload\":{\"payment\":{\"entity\":{\"id\":\"" + paymentId
            + "\",\"order_id\":\"" + p.getRazorpayOrderId() + "\",\"notes\":{\"ride_id\":\"10\"}}}}}").getBytes(StandardCharsets.UTF_8);
        service.handleWebhook(body, PaymentFlowIntegrationTest.signature(body, PaymentFlowIntegrationTest.FakeRazorpay.WEBHOOK_SECRET));
    }
    private Runnable block(CountDownLatch entered, CountDownLatch released) {
        return () -> {
            entered.countDown();
            try { assertTrue(released.await(10, TimeUnit.SECONDS)); }
            catch (InterruptedException ex) { Thread.currentThread().interrupt(); throw new IllegalStateException(ex); }
        };
    }
    private void assertRowAvailable(ExecutorService workers, PaymentStatus status) throws Exception {
        var read = workers.submit(() -> work.execute(() -> repository.getLockedByRideId(10L)));
        assertEquals(status, read.get(2, TimeUnit.SECONDS).getStatus(), "A stalled provider must not hold the payment row lock");
    }

    @Test void slowOrderCreationCommitsItsClaimWithoutHoldingDatabaseTransactionOrLock() throws Exception {
        CountDownLatch entered = new CountDownLatch(1), released = new CountDownLatch(1);
        provider.createHook = block(entered, released);
        try (var workers = Executors.newFixedThreadPool(2)) {
            Future<?> request = workers.submit(() -> service.handleRideComplete(event()));
            try {
                assertTrue(entered.await(5, TimeUnit.SECONDS));
                assertRowAvailable(workers, PaymentStatus.PENDING);
                var durable = payments.findById(10L).orElseThrow();
                assertNotNull(durable.getProviderOperationToken());
                assertNotNull(durable.getOrderCreationAttemptedAt());
            } finally { released.countDown(); }
            request.get(5, TimeUnit.SECONDS);
        }
        assertEquals(1, provider.creates.get());
        assertNull(payments.findById(10L).orElseThrow().getProviderOperationToken());
    }

    @Test void slowProviderFetchAndCaptureEachLeaveThePaymentRowAvailable() throws Exception {
        Payment p = create(); provider.setPayment(p, "pay_slow", "authorized");
        for (boolean capture : new boolean[] {false, true}) {
            CountDownLatch entered = new CountDownLatch(1), released = new CountDownLatch(1);
            provider.setPayment(p, "pay_slow", "authorized");
            provider.capturePending = true;
            AtomicBoolean once = new AtomicBoolean();
            Runnable blocking = () -> { if (once.compareAndSet(false, true)) block(entered, released).run(); };
            if (capture) provider.captureHook = blocking; else provider.fetchHook = blocking;
            try (var workers = Executors.newFixedThreadPool(2)) {
                Future<Payment> request = workers.submit(() -> service.verifyPayment(10L, 100L, verification(p, "pay_slow")));
                try {
                    assertTrue(entered.await(5, TimeUnit.SECONDS));
                    assertRowAvailable(workers, PaymentStatus.ORDER_CREATED);
                } finally { released.countDown(); }
                assertEquals(PaymentStatus.ORDER_CREATED, request.get(5, TimeUnit.SECONDS).getStatus());
            }
            provider.fetchHook = provider.captureHook = null;
        }
    }

    @Test void callerTransactionIsSuspendedAroundProviderCalls() {
        new TransactionTemplate(manager).execute(status -> {
            service.handleRideComplete(event());
            Payment p = service.getRidePayment(10L, 100L);
            provider.setPayment(p, "pay_outer", "authorized");
            assertEquals(PaymentStatus.COMPLETED, service.verifyPayment(10L, 100L, verification(p, "pay_outer")).getStatus());
            return null;
        });
        assertEquals(PaymentStatus.COMPLETED, payments.findById(10L).orElseThrow().getStatus());
    }

    @Test void separateServiceInstancesShareDurableClaimAndCreateOnlyOneOrder() throws Exception {
        PaymentService anotherInstance = new PaymentService(repository, work, provider, rides, events, mapper);
        CountDownLatch entered = new CountDownLatch(1), released = new CountDownLatch(1);
        provider.createHook = block(entered, released);
        try (var workers = Executors.newFixedThreadPool(2)) {
            Future<?> first = workers.submit(() -> service.handleRideComplete(event()));
            try {
                assertTrue(entered.await(5, TimeUnit.SECONDS));
                Future<Payment> second = workers.submit(() -> anotherInstance.createOrder(10L, 100L));
                released.countDown();
                first.get(5, TimeUnit.SECONDS);
                assertEquals(PaymentStatus.ORDER_CREATED, second.get(5, TimeUnit.SECONDS).getStatus());
            } finally { released.countDown(); }
        }
        assertEquals(1, provider.creates.get()); assertEquals(1, orders.count()); assertEquals(1, outbox.count());
    }

    @Test void capturedWebhookDuringOrderPostWinsAndLatePostCannotRegressCompletion() throws Exception {
        CountDownLatch entered = new CountDownLatch(1), released = new CountDownLatch(1);
        provider.createHook = block(entered, released);
        try (var workers = Executors.newFixedThreadPool(2)) {
            Future<?> request = workers.submit(() -> service.handleRideComplete(event()));
            try {
                assertTrue(entered.await(5, TimeUnit.SECONDS));
                Payment p = service.getRidePayment(10L, 100L);
                p.setRazorpayOrderId(provider.providerOrders.get(p.getReceipt()).orderId());
                provider.setPayment(p, "pay_early_webhook", "captured");
                workers.submit(() -> webhook(p, "pay_early_webhook")).get(2, TimeUnit.SECONDS);
                assertEquals(PaymentStatus.COMPLETED, payments.findById(10L).orElseThrow().getStatus());
            } finally { released.countDown(); }
            request.get(5, TimeUnit.SECONDS);
        }
        assertEquals(PaymentStatus.COMPLETED, payments.findById(10L).orElseThrow().getStatus());
        assertEquals(1, provider.creates.get()); assertEquals(2, outbox.count());
    }

    @Test void capturedWebhookDuringCaptureWinsAndLatePendingCaptureCannotRegressCompletion() throws Exception {
        Payment p = create(); provider.setPayment(p, "pay_capture_race", "authorized"); provider.capturePending = true;
        CountDownLatch entered = new CountDownLatch(1), released = new CountDownLatch(1);
        provider.captureHook = block(entered, released);
        try (var workers = Executors.newFixedThreadPool(2)) {
            Future<Payment> request = workers.submit(() -> service.verifyPayment(10L, 100L, verification(p, "pay_capture_race")));
            try {
                assertTrue(entered.await(5, TimeUnit.SECONDS));
                provider.setPayment(p, "pay_capture_race", "captured");
                workers.submit(() -> webhook(p, "pay_capture_race")).get(2, TimeUnit.SECONDS);
            } finally { released.countDown(); }
            assertEquals(PaymentStatus.COMPLETED, request.get(5, TimeUnit.SECONDS).getStatus());
        }
        assertEquals(2, outbox.count()); assertEquals("pay_capture_race", payments.findById(10L).orElseThrow().getRazorpayPaymentId());
    }

    @Test void capturedWebhookDuringFetchWinsAndLateFailedSnapshotCannotRegressCompletion() throws Exception {
        Payment p = create(); provider.setPayment(p, "pay_fetch_race", "failed");
        CountDownLatch entered = new CountDownLatch(1), released = new CountDownLatch(1);
        AtomicBoolean once = new AtomicBoolean();
        provider.fetchHook = () -> { if (once.compareAndSet(false, true)) block(entered, released).run(); };
        try (var workers = Executors.newFixedThreadPool(2)) {
            Future<Payment> request = workers.submit(() -> service.verifyPayment(10L, 100L, verification(p, "pay_fetch_race")));
            try {
                assertTrue(entered.await(5, TimeUnit.SECONDS));
                provider.setPayment(p, "pay_fetch_race", "captured");
                workers.submit(() -> webhook(p, "pay_fetch_race")).get(2, TimeUnit.SECONDS);
            } finally { released.countDown(); }
            assertEquals(PaymentStatus.COMPLETED, request.get(5, TimeUnit.SECONDS).getStatus());
        }
        assertEquals(2, outbox.count());
    }

    @Test void ambiguousCreationWithEmptyReceiptLookupNeverPostsAnotherOrder() {
        provider.crashAfterCreate = true;
        assertThrows(PaymentException.class, () -> service.handleRideComplete(event()));
        String receipt = payments.findById(10L).orElseThrow().getReceipt();
        provider.providerOrders.clear(); provider.crashAfterCreate = false;
        PaymentException failure = assertThrows(PaymentException.class, () -> service.createOrder(10L, 100L));
        assertEquals(PaymentException.Kind.UNAVAILABLE, failure.getKind());
        assertEquals(1, provider.creates.get()); assertEquals(receipt, payments.findById(10L).orElseThrow().getReceipt());
        assertEquals(PaymentStatus.PENDING, payments.findById(10L).orElseThrow().getStatus());
        assertNotNull(payments.findById(10L).orElseThrow().getOrderCreationAttemptedAt());
    }

    @Test void retryDuringFetchCannotBeOverwrittenByAnOlderFailedSnapshot() throws Exception {
        Payment p = create(); provider.setPayment(p, "pay_old_failure", "failed");
        service.verifyPayment(10L, 100L, verification(p, "pay_old_failure"));
        assertEquals(PaymentStatus.FAILED, payments.findById(10L).orElseThrow().getStatus());
        CountDownLatch entered = new CountDownLatch(1), released = new CountDownLatch(1);
        provider.fetchHook = block(entered, released);
        try (var workers = Executors.newFixedThreadPool(2)) {
            Future<Payment> request = workers.submit(() -> service.verifyPayment(10L, 100L, verification(p, "pay_old_failure")));
            try {
                assertTrue(entered.await(5, TimeUnit.SECONDS));
                assertEquals(PaymentStatus.ORDER_CREATED, service.createOrder(10L, 100L).getStatus());
            } finally { released.countDown(); }
            assertEquals(PaymentStatus.ORDER_CREATED, request.get(5, TimeUnit.SECONDS).getStatus());
        }
        assertEquals(1, provider.creates.get()); assertEquals(3, outbox.count());
    }

    @Test void expiredClaimRecoversPersistedReceiptWithoutCreatingSecondOrder() {
        provider.crashAfterCreate = true;
        assertThrows(PaymentException.class, () -> service.handleRideComplete(event()));
        work.execute(() -> {
            Payment p = repository.getLockedByRideId(10L);
            p.setProviderOperationToken("abandoned_instance"); p.setProviderOperationExpiresAt(Instant.now().minusSeconds(1));
            repository.save(p); return null;
        });
        provider.crashAfterCreate = false;
        assertEquals(PaymentStatus.ORDER_CREATED, service.createOrder(10L, 100L).getStatus());
        assertEquals(1, provider.creates.get()); assertNull(payments.findById(10L).orElseThrow().getProviderOperationToken());
    }

    @Test void expiredClaimWithUnknownOutcomeAndEmptyLookupCannotCreateAnotherOrder() {
        service.getRidePayment(10L, 100L);
        work.execute(() -> {
            Payment p = repository.getLockedByRideId(10L);
            p.setProviderOperationToken("dead_instance"); p.setProviderOperationExpiresAt(Instant.now().minusSeconds(1));
            p.setOrderCreationAttemptedAt(Instant.now().minusSeconds(200)); repository.save(p); return null;
        });
        assertEquals(PaymentException.Kind.UNAVAILABLE, assertThrows(PaymentException.class,
            () -> service.createOrder(10L, 100L)).getKind());
        assertEquals(0, provider.creates.get());
    }

    @Test void definiteProviderRejectionReleasesAttemptAndAllowsSafeRetry() {
        provider.createFailure = new OrderCreationRejectedException(PaymentException.Kind.PROVIDER_FAILURE, "Offline definite rejection");
        assertThrows(PaymentException.class, () -> service.handleRideComplete(event()));
        var p = payments.findById(10L).orElseThrow();
        assertNull(p.getOrderCreationAttemptedAt()); assertNull(p.getProviderOperationToken());
        provider.createFailure = null;
        assertEquals(PaymentStatus.ORDER_CREATED, service.createOrder(10L, 100L).getStatus());
        assertEquals(1, provider.creates.get());
    }

    @Test void lostCaptureReplyRecoversCapturedFundsWithoutCapturingAgain() {
        Payment p = create(); provider.setPayment(p, "pay_lost_capture_reply", "authorized");
        provider.captureHook = () -> { throw new PaymentException(PaymentException.Kind.PROVIDER_FAILURE, "Offline capture reply lost"); };
        assertThrows(PaymentException.class, () -> service.verifyPayment(10L, 100L, verification(p, "pay_lost_capture_reply")));
        assertEquals(PaymentStatus.ORDER_CREATED, payments.findById(10L).orElseThrow().getStatus());
        assertNull(payments.findById(10L).orElseThrow().getProviderOperationToken());
        provider.captureHook = null;
        assertEquals(PaymentStatus.COMPLETED, service.verifyPayment(10L, 100L, verification(p, "pay_lost_capture_reply")).getStatus());
        assertEquals(1, provider.captures.get()); assertEquals(2, outbox.count());
    }

    @Test void legacyOrderlessPaymentWithEmptyLookupCannotBlindlyCreateAfterUpgrade() {
        Payment p = service.getRidePayment(10L, 100L);
        work.execute(() -> {
            Payment legacy = repository.getLockedByRideId(10L);
            legacy.setProviderWorkflowVersion(null); repository.save(legacy); return null;
        });
        for (int attempt = 0; attempt < 2; attempt++) {
            assertEquals(PaymentException.Kind.UNAVAILABLE, assertThrows(PaymentException.class,
                () -> service.createOrder(10L, 100L)).getKind());
        }
        var stored = payments.findById(10L).orElseThrow();
        assertEquals(PaymentStatus.PENDING, stored.getStatus()); assertEquals(p.getReceipt(), stored.getReceipt());
        assertNull(stored.getProviderWorkflowVersion()); assertNull(stored.getProviderOperationToken());
        assertEquals(0, provider.creates.get()); assertEquals(0, orders.count()); assertEquals(0, outbox.count());
    }

    @Test void legacyPaymentCanRecoverItsExistingProviderOrderWithoutPostingAgain() {
        Payment p = service.getRidePayment(10L, 100L);
        work.execute(() -> {
            Payment legacy = repository.getLockedByRideId(10L);
            legacy.setProviderWorkflowVersion(null); repository.save(legacy); return null;
        });
        provider.providerOrders.put(p.getReceipt(), new ProviderOrder("order_from_old_version", p.getAmount(), p.getCurrency(), p.getReceipt()));
        Payment recovered = service.createOrder(10L, 100L);
        assertEquals(PaymentStatus.ORDER_CREATED, recovered.getStatus());
        assertEquals("order_from_old_version", recovered.getRazorpayOrderId()); assertEquals(p.getReceipt(), recovered.getReceipt());
        assertEquals(0, provider.creates.get()); assertEquals(1, orders.count()); assertEquals(1, outbox.count());
    }

    @Test void additiveMigrationIsIdempotentAndKeepsLegacyProviderHistoryUnknown() throws Exception {
        // An isolated schema exercises the actual upgrade file without touching application rows.
        String schema = "provider_upgrade_" + UUID.randomUUID().toString().replace("-", "");
        try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            String originalSchema = connection.getSchema();
            statement.execute("CREATE SCHEMA \"" + schema + "\"");
            try {
                connection.setSchema(schema);
                statement.execute("CREATE TABLE ride_payments (ride_id BIGINT PRIMARY KEY, receipt VARCHAR(40), status VARCHAR(20))");
                statement.execute("INSERT INTO ride_payments (ride_id, receipt, status) VALUES (10, 'durable_legacy_receipt', 'PENDING')");
                var script = new ClassPathResource("db/upgrades/20261014_payment_provider_operations.sql");
                ScriptUtils.executeSqlScript(connection, script);
                ScriptUtils.executeSqlScript(connection, script);
                try (var row = statement.executeQuery("SELECT receipt, status, provider_workflow_version, order_creation_attempted_at, provider_operation_token, provider_operation_expires_at FROM ride_payments WHERE ride_id = 10")) {
                    assertTrue(row.next()); assertEquals("durable_legacy_receipt", row.getString(1)); assertEquals("PENDING", row.getString(2));
                    for (int column = 3; column <= 6; column++) assertNull(row.getObject(column));
                }
                statement.execute("INSERT INTO ride_payments (ride_id, receipt, status) VALUES (11, 'another_old_writer', 'PENDING')");
                try (var row = statement.executeQuery("SELECT provider_workflow_version FROM ride_payments WHERE ride_id = 11")) {
                    assertTrue(row.next()); assertNull(row.getObject(1), "A database default must not falsely label legacy writes as protocol version 1");
                }
            } finally {
                connection.setSchema(originalSchema);
                statement.execute("DROP SCHEMA \"" + schema + "\" CASCADE");
            }
        }
    }
}
