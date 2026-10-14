package com.swiftapp_backend.paymentservice.payment.application.service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftapp_backend.paymentservice.payment.application.port.*;
import com.swiftapp_backend.paymentservice.payment.domain.enums.PaymentStatus;
import com.swiftapp_backend.paymentservice.payment.domain.event.*;
import com.swiftapp_backend.paymentservice.payment.domain.exception.PaymentException;
import com.swiftapp_backend.paymentservice.payment.domain.exception.OrderCreationRejectedException;
import com.swiftapp_backend.paymentservice.payment.domain.model.*;
import com.swiftapp_backend.paymentservice.payment.domain.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.Duration;
import java.util.function.Supplier;
import java.util.Objects;
import java.util.UUID;
import static com.swiftapp_backend.paymentservice.payment.domain.exception.PaymentException.Kind.*;
@Service @RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository payments;
    private final PaymentUnitOfWork transactions;
    private final RazorpayPort razorpay;
    private final RideLookupPort rides;
    private final PaymentEventPort events;
    private final ObjectMapper mapper;
    private static final Duration PROVIDER_LEASE = Duration.ofMinutes(3);
    private static final Duration COORDINATION_WAIT = Duration.ofSeconds(10);

    private record ProviderOperation(Payment payment, String token) {}

    public void handleRideComplete(RideCompletedEvent event) {
        transactions.outside(() -> {
            initialize(event);
            createActiveOrder(event.rideId(), false);
            return null;
        });
    }

    public Payment getRidePayment(Long rideId, Long authUserId) {
        return transactions.outside(() -> {
            CompletedRide ride = rides.getParticipantRide(rideId, authUserId);
            requireCompleted(ride);
            initialize(event(ride));
            return payments.getByRideId(rideId);
        });
    }

    public Payment createOrder(Long rideId, Long authUserId) {
        return transactions.outside(() -> {
            CompletedRide ride = requireRider(rideId, authUserId);
            requireCompleted(ride);
            initialize(event(ride));
            return createActiveOrder(rideId, true);
        });
    }

    public Payment verifyPayment(Long rideId, Long authUserId, VerifyPaymentCommand request) {
        return transactions.outside(() -> {
            CompletedRide ride = requireRider(rideId, authUserId);
            requireCompleted(ride);
            Payment snapshot = transactions.execute(() -> {
                Payment payment = locked(rideId);
                if (!Objects.equals(payment.getRiderId(), ride.riderId()))
                    throw error(FORBIDDEN, "Ride payment belongs to another rider");
                requireRecordedOrder(request.razorpayOrderId(), rideId);
                return payment;
            });
            if (!razorpay.verifyCheckoutSignature(request.razorpayOrderId(), request.razorpayPaymentId(), request.razorpaySignature()))
                throw error(INVALID_REQUEST, "Invalid payment signature");
            if (snapshot.getStatus() == PaymentStatus.COMPLETED) {
                requireSameCompletedPayment(snapshot, request.razorpayPaymentId());
                return snapshot;
            }
            return reconcileProviderPayment(snapshot, request.razorpayOrderId(), request.razorpayPaymentId(), true);
        });
    }

    public void handleWebhook(byte[] body, String signature) {
        transactions.outside(() -> { processWebhook(body, signature); return null; });
    }

    private void processWebhook(byte[] body, String signature) {
        if (!razorpay.verifyWebhookSignature(body, signature)) throw error(INVALID_REQUEST, "Invalid webhook signature");
        JsonNode webhook;
        try { webhook = mapper.readTree(body); }
        catch (java.io.IOException ex) { throw error(INVALID_REQUEST, "Invalid webhook payload"); }
        String type = webhook.path("event").asText();
        if (!java.util.Set.of("payment.captured", "payment.authorized", "payment.failed", "order.paid").contains(type)) return;
        JsonNode entity = webhook.path("payload").path("payment").path("entity");
        String paymentId = entity.path("id").asText();
        String orderId = entity.path("order_id").asText();
        if (paymentId.isBlank() || orderId.isBlank()) throw error(INVALID_REQUEST, "Webhook payment identifiers are missing");
        Long rideId = payments.rideIdForOrder(orderId);
        if (rideId == null) {
            long notedRide = entity.path("notes").path("ride_id").asLong();
            Payment snapshot = notedRide > 0 ? payments.getByRideId(notedRide) : null;
            if (snapshot == null) return;
            // A webhook can arrive before the POST response is committed. Recover directly by receipt;
            // do not wait on or steal the in-flight order operation.
            ProviderOrder recovered = razorpay.findOrderByReceipt(snapshot.getReceipt());
            if (recovered == null || !Objects.equals(recovered.orderId(), orderId))
                throw error(NOT_FOUND, "Ride payment order has not been persisted yet");
            validateOrder(snapshot, recovered);
            transactions.execute(() -> persistRecoveredOrder(notedRide, recovered));
            rideId = payments.rideIdForOrder(orderId);
        }
        Long targetRideId = rideId;
        Payment snapshot = transactions.execute(() -> {
            Payment payment = locked(targetRideId);
            requireRecordedOrder(orderId, targetRideId);
            return payment;
        });
        if (snapshot.getStatus() != PaymentStatus.COMPLETED)
            reconcileProviderPayment(snapshot, orderId, paymentId, false);
    }

    private Payment reconcileProviderPayment(Payment snapshot, String orderId, String paymentId, boolean checkout) {
        ProviderPayment provider = razorpay.fetchPayment(paymentId);
        validateProvider(snapshot, orderId, paymentId, provider);
        if (!"authorized".equals(provider.status()))
            return applyReconciled(snapshot.getRideId(), orderId, paymentId, provider, checkout, null, snapshot.getUpdatedAt());

        ProviderOperation operation = awaitOperation(() -> transactions.execute(() -> {
            Payment payment = locked(snapshot.getRideId());
            requireRecordedOrder(orderId, payment.getRideId());
            if (payment.getStatus() == PaymentStatus.COMPLETED) {
                if (checkout) requireSameCompletedPayment(payment, paymentId);
                return new ProviderOperation(payment, null);
            }
            return claim(payment);
        }));
        if (operation.token() == null) return operation.payment();
        try {
            // Refetch after claiming: another verifier/webhook may already have captured the funds.
            provider = razorpay.fetchPayment(paymentId);
            validateProvider(operation.payment(), orderId, paymentId, provider);
            if ("authorized".equals(provider.status())) {
                provider = razorpay.capturePayment(paymentId, operation.payment().getAmount(), operation.payment().getCurrency());
                validateProvider(operation.payment(), orderId, paymentId, provider);
            }
            return applyReconciled(snapshot.getRideId(), orderId, paymentId, provider, checkout, operation.token(), operation.payment().getUpdatedAt());
        } finally {
            releaseOperation(snapshot.getRideId(), operation.token(), false);
        }
    }

    private Payment applyReconciled(Long rideId, String orderId, String paymentId, ProviderPayment provider,
                                    boolean checkout, String token, Instant observedStateAt) {
        return transactions.execute(() -> {
            Payment payment = locked(rideId);
            requireRecordedOrder(orderId, rideId);
            validateProvider(payment, orderId, paymentId, provider);
            if (payment.getStatus() == PaymentStatus.COMPLETED) {
                if (checkout) requireSameCompletedPayment(payment, paymentId);
                return payment;
            }
            if (token != null && !Objects.equals(token, payment.getProviderOperationToken()))
                throw error(UNAVAILABLE, "Payment verification is being reconciled; please check its status");
            // Retry may have reset a failed attempt while this remote fetch was in flight.
            // Captured funds are definitive; an older failure must not overwrite the newer retry.
            if ("failed".equals(provider.status()) && !Objects.equals(observedStateAt, payment.getUpdatedAt())) return payment;
            return applyProviderStatus(payment, provider);
        });
    }

    private void requireRecordedOrder(String orderId, Long rideId) {
        if (!payments.orderBelongsToRide(orderId, rideId)) throw error(INVALID_REQUEST, "Order does not match this ride");
    }

    private void requireSameCompletedPayment(Payment payment, String paymentId) {
        if (!Objects.equals(payment.getRazorpayPaymentId(), paymentId))
            throw error(CONFLICT, "This ride already has a completed payment");
    }

    public String razorpayKeyId() { return razorpay.keyId(); }

    private CompletedRide requireRider(Long rideId, Long authUserId) {
        CompletedRide ride = rides.getParticipantRide(rideId, authUserId);
        if (!Objects.equals(rides.riderIdForAuthUser(authUserId), ride.riderId())) throw error(FORBIDDEN, "Only this ride's rider may pay");
        return ride;
    }
    private void requireCompleted(CompletedRide ride) {
        if (!ride.completed()) throw error(CONFLICT, "The ride must be completed before payment");
    }
    private RideCompletedEvent event(CompletedRide ride) {
        return new RideCompletedEvent(ride.rideId(), ride.riderId(), ride.driverId(), ride.totalFare(), ride.currency(), ride.completedAt());
    }
    private void initialize(RideCompletedEvent event) {
        validateEvent(event);
        try {
            transactions.execute(() -> {
                Payment payment = payments.getLockedByRideId(event.rideId());
                if (payment == null) {
                    Instant now = Instant.now();
                    payment = Payment.builder().rideId(event.rideId()).riderId(event.riderId()).driverId(event.driverId())
                        .amount(minorAmount(event.totalFare())).currency(currency(event.currency())).receipt(receipt())
                        .status(PaymentStatus.PENDING).createdAt(now).updatedAt(now).providerWorkflowVersion(1).build();
                    payments.insert(payment);
                } else validateEventMatchesPayment(event, payment);
                return payment;
            });
        } catch (DataIntegrityViolationException ex) {
            // Concurrent first delivery/order requests may both see no row; the primary key chooses one.
            Payment winner = payments.getByRideId(event.rideId());
            if (winner == null) throw ex;
            validateEventMatchesPayment(event, winner);
        }
    }
    private Payment createActiveOrder(Long rideId, boolean allowRetry) {
        ProviderOperation operation = awaitOperation(() -> transactions.execute(() -> {
            Payment payment = locked(rideId);
            if (payment.getRazorpayOrderId() != null || payment.getStatus() == PaymentStatus.COMPLETED) {
                if (payment.getStatus() == PaymentStatus.FAILED && allowRetry)
                    transition(payment, PaymentStatus.ORDER_CREATED, null);
                return new ProviderOperation(payment, null);
            }
            return claim(payment);
        }));
        if (operation.token() == null) return operation.payment();
        boolean definiteRejection = false;
        try {
            Payment snapshot = operation.payment();
            ProviderOrder order = razorpay.findOrderByReceipt(snapshot.getReceipt());
            if (order == null) {
                snapshot = transactions.execute(() -> {
                    Payment payment = locked(rideId);
                    requireOwner(payment, operation.token());
                    if (payment.getRazorpayOrderId() != null || payment.getStatus() == PaymentStatus.COMPLETED) return payment;
                    if (!Integer.valueOf(1).equals(payment.getProviderWorkflowVersion()))
                        throw error(UNAVAILABLE, "This earlier payment request requires provider reconciliation; please check status or retry later");
                    if (payment.getOrderCreationAttemptedAt() != null)
                        throw error(UNAVAILABLE, "The previous Razorpay order request is being reconciled; please check status or retry later");
                    // Durable before the POST. A timeout or crash cannot safely authorize a second POST.
                    payment.setOrderCreationAttemptedAt(Instant.now());
                    payments.save(payment);
                    return payment;
                });
                if (snapshot.getRazorpayOrderId() != null || snapshot.getStatus() == PaymentStatus.COMPLETED) return snapshot;
                try { order = razorpay.createOrder(snapshot); }
                catch (OrderCreationRejectedException ex) { definiteRejection = true; throw ex; }
            }
            validateOrder(snapshot, order);
            ProviderOrder result = order;
            return transactions.execute(() -> {
                Payment payment = locked(rideId);
                if (payment.getStatus() == PaymentStatus.COMPLETED) return payment;
                requireOwner(payment, operation.token());
                return persistRecoveredOrder(payment, result);
            });
        } finally {
            releaseOperation(rideId, operation.token(), definiteRejection);
        }
    }

    private Payment persistRecoveredOrder(Long rideId, ProviderOrder order) {
        return persistRecoveredOrder(locked(rideId), order);
    }

    private Payment persistRecoveredOrder(Payment payment, ProviderOrder order) {
        validateOrder(payment, order);
        if (payment.getRazorpayOrderId() != null) {
            if (!Objects.equals(payment.getRazorpayOrderId(), order.orderId()))
                throw error(CONFLICT, "The ride already has another payment order");
            return payment;
        }
        if (payment.getStatus() == PaymentStatus.COMPLETED) return payment;
        payment.setRazorpayOrderId(order.orderId());
        payments.recordOrder(payment);
        return transition(payment, PaymentStatus.ORDER_CREATED, null);
    }

    private void validateOrder(Payment payment, ProviderOrder order) {
        if (order == null || order.orderId() == null || order.orderId().isBlank() || order.amount() != payment.getAmount()
            || !Objects.equals(order.currency(), payment.getCurrency()) || !Objects.equals(order.receipt(), payment.getReceipt()))
            throw error(PROVIDER_FAILURE, "Razorpay order details do not match the ride");
    }

    private ProviderOperation claim(Payment payment) {
        Instant now = Instant.now();
        if (payment.getProviderOperationToken() != null && payment.getProviderOperationExpiresAt() != null
            && payment.getProviderOperationExpiresAt().isAfter(now)) return null;
        String token = UUID.randomUUID().toString();
        payment.setProviderOperationToken(token);
        payment.setProviderOperationExpiresAt(now.plus(PROVIDER_LEASE));
        payments.save(payment);
        return new ProviderOperation(payment, token);
    }

    private ProviderOperation awaitOperation(Supplier<ProviderOperation> attempt) {
        long deadline = System.nanoTime() + COORDINATION_WAIT.toNanos();
        long waitMillis = 25;
        do {
            ProviderOperation operation = attempt.get();
            if (operation != null) return operation;
            if (System.nanoTime() >= deadline)
                throw error(UNAVAILABLE, "Another payment request is in progress; please check status or retry shortly");
            try { Thread.sleep(waitMillis); waitMillis = Math.min(250, waitMillis * 2); }
            catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw error(UNAVAILABLE, "Payment request interrupted; please check status");
            }
        } while (true);
    }

    private void requireOwner(Payment payment, String token) {
        if (!Objects.equals(token, payment.getProviderOperationToken()))
            throw error(UNAVAILABLE, "Payment request lease changed; please check status or retry");
    }

    private void releaseOperation(Long rideId, String token, boolean clearCreationAttempt) {
        transactions.execute(() -> {
            Payment payment = locked(rideId);
            if (Objects.equals(token, payment.getProviderOperationToken())) {
                payment.setProviderOperationToken(null);
                payment.setProviderOperationExpiresAt(null);
                if (clearCreationAttempt && payment.getRazorpayOrderId() == null)
                    payment.setOrderCreationAttemptedAt(null);
                payments.save(payment);
            }
            return payment;
        });
    }

    private Payment applyProviderStatus(Payment payment, ProviderPayment provider) {
        if ("captured".equals(provider.status())) {
            payment.setRazorpayOrderId(provider.orderId());
            return transition(payment, PaymentStatus.COMPLETED, provider.paymentId());
        }
        if ("failed".equals(provider.status()) && Objects.equals(payment.getRazorpayOrderId(), provider.orderId()))
            return transition(payment, PaymentStatus.FAILED, provider.paymentId());
        // A checkout callback is not proof of captured funds; pending states must not report success.
        return payment;
    }
    private Payment transition(Payment payment, PaymentStatus status, String providerPaymentId) {
        if (payment.getStatus() == PaymentStatus.COMPLETED) return payment;
        if (payment.getStatus() == status && Objects.equals(payment.getRazorpayPaymentId(), providerPaymentId)) return payment;
        payment.setStatus(status); payment.setRazorpayPaymentId(providerPaymentId); payment.setUpdatedAt(Instant.now());
        payments.save(payment);
        String topic = switch (status) {
            case ORDER_CREATED -> "payment.order.created";
            case COMPLETED -> "payment.completed";
            case FAILED -> "payment.failed";
            default -> throw new IllegalStateException("No event for pending payment");
        };
        events.enqueue(topic, new PaymentStatusEvent(UUID.randomUUID().toString(), payment.getRideId(), payment.getRiderId(),
            payment.getDriverId(), status.name(), payment.getRazorpayOrderId(), payment.getRazorpayPaymentId(),
            payment.getAmount(), payment.getCurrency(), payment.getUpdatedAt()));
        return payment;
    }
    private Payment locked(Long rideId) {
        Payment payment = payments.getLockedByRideId(rideId);
        if (payment == null) throw error(NOT_FOUND, "Payment order has not been created for this ride");
        return payment;
    }
    private void validateProvider(Payment payment, String orderId, String paymentId, ProviderPayment provider) {
        if (!Objects.equals(provider.orderId(), orderId) || provider.amount() != payment.getAmount()
            || !Objects.equals(provider.currency(), payment.getCurrency()) || !Objects.equals(provider.paymentId(), paymentId))
            throw error(INVALID_REQUEST, "Provider payment details do not match the ride");
    }
    private void validateEvent(RideCompletedEvent event) {
        if (event == null || event.rideId() == null || event.rideId() <= 0 || event.riderId() == null || event.riderId() <= 0
            || event.driverId() == null || event.driverId() <= 0 || event.totalFare() == null)
            throw error(INVALID_REQUEST, "Ride completion event is invalid");
        currency(event.currency()); minorAmount(event.totalFare());
    }
    private void validateEventMatchesPayment(RideCompletedEvent event, Payment payment) {
        if (!Objects.equals(payment.getRiderId(), event.riderId()) || !Objects.equals(payment.getDriverId(), event.driverId())
            || payment.getAmount() != minorAmount(event.totalFare()) || !Objects.equals(payment.getCurrency(), currency(event.currency())))
            throw error(CONFLICT, "Ride completion details conflict with the recorded payment");
    }
    private String currency(String currency) {
        if (currency == null || currency.isBlank()) throw error(INVALID_REQUEST, "Ride currency is required");
        if (!"INR".equals(currency)) throw error(INVALID_REQUEST, "Ride payments currently support INR only");
        return currency;
    }
    private long minorAmount(BigDecimal fare) {
        try {
            long amount = fare.movePointRight(2).longValueExact();
            if (amount < 100) throw error(INVALID_REQUEST, "Ride fare must be at least INR 1.00");
            return amount;
        } catch (ArithmeticException ex) { throw error(INVALID_REQUEST, "Ride fare must have at most two decimal places"); }
    }
    private String receipt() { return "ride_" + UUID.randomUUID().toString().replace("-", ""); }
    private PaymentException error(PaymentException.Kind kind, String message) { return new PaymentException(kind, message); }
}
