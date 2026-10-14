package com.swiftapp_backend.paymentservice.payment;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftapp_backend.paymentservice.payment.application.service.PaymentService;
import com.swiftapp_backend.paymentservice.payment.domain.event.RideCompletedEvent;
import com.swiftapp_backend.paymentservice.payment.infrastructure.kafka.service.KafkaConsumerService;
import com.swiftapp_backend.paymentservice.shared.GlobalExceptionHandler;
import com.swiftapp_backend.paymentservice.shared.SafeFailureDetails;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class PaymentFailureDiagnosticsTest {
    private static final String SENSITIVE = "secret_provider_response_or_request_payload";

    @Test
    void exceptionDetailsKeepTypesAndLocationsWithoutMessagesOrSuppressedData() {
        IllegalStateException failure = new IllegalStateException(SENSITIVE, new ClassCastException(SENSITIVE));
        failure.addSuppressed(new RuntimeException(SENSITIVE));

        String details = SafeFailureDetails.describe(failure);

        assertTrue(details.contains("java.lang.IllegalStateException"));
        assertTrue(details.contains("java.lang.ClassCastException"));
        assertTrue(details.contains("PaymentFailureDiagnosticsTest.java:"));
        assertFalse(details.contains(SENSITIVE));
    }

    @Test
    void unexpectedRestFailureLogsSafeDetailsAndKeepsGenericResponse() {
        Logger logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
        ListAppender<ILoggingEvent> appender = attach(logger);
        try {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/payments/rides/10/order");
            request.addHeader("Authorization", SENSITIVE);
            var response = new GlobalExceptionHandler().unexpectedError(new ClassCastException(SENSITIVE), request);

            assertEquals(500, response.getStatusCode().value());
            assertEquals("Payment processing failed; please retry", response.getBody().message());
            String logged = appender.list.getFirst().getFormattedMessage();
            assertTrue(logged.contains("endpoint=/api/v1/payments/rides/10/order"));
            assertTrue(logged.contains("java.lang.ClassCastException"));
            assertFalse(logged.contains(SENSITIVE));
            assertNull(appender.list.getFirst().getThrowableProxy());
        } finally {
            logger.detachAppender(appender);
        }
    }

    @Test
    void malformedKafkaRecordLogsLocationWithoutPayloadAndRemainsUnacknowledged() {
        Logger logger = (Logger) LoggerFactory.getLogger(KafkaConsumerService.class);
        ListAppender<ILoggingEvent> appender = attach(logger);
        try {
            KafkaConsumerService consumer = new KafkaConsumerService(new ObjectMapper(), null);
            var record = new ConsumerRecord<String, String>("ride.completed", 0, 5L, "10", SENSITIVE);

            assertThrows(IOException.class, () -> consumer.consume(record));

            String logged = appender.list.getFirst().getFormattedMessage();
            assertTrue(logged.contains("topic=ride.completed partition=0 offset=5"));
            assertTrue(logged.contains("JsonParseException"));
            assertFalse(logged.contains(SENSITIVE));
            assertNull(appender.list.getFirst().getThrowableProxy());
        } finally {
            logger.detachAppender(appender);
        }
    }

    @Test
    void runtimeKafkaFailureIsRethrownAndLogsSafeCodeLocations() {
        Logger logger = (Logger) LoggerFactory.getLogger(KafkaConsumerService.class);
        ListAppender<ILoggingEvent> appender = attach(logger);
        try {
            ObjectMapper mapper = new ObjectMapper();
            ClassCastException failure = new ClassCastException(SENSITIVE);
            PaymentService failing = new PaymentService(null, null, null, null, null, mapper) {
                @Override public void handleRideComplete(RideCompletedEvent event) { throw failure; }
            };
            KafkaConsumerService consumer = new KafkaConsumerService(mapper, failing);
            var record = new ConsumerRecord<String, String>("ride.completed", 0, 5L, "10",
                "{\"rideId\":10,\"riderId\":20,\"driverId\":30,\"totalFare\":123.45,\"currency\":\"INR\"}");

            assertSame(failure, assertThrows(ClassCastException.class, () -> consumer.consume(record)));

            String logged = appender.list.getFirst().getFormattedMessage();
            assertTrue(logged.contains("java.lang.ClassCastException"));
            assertTrue(logged.contains("PaymentFailureDiagnosticsTest.java:"));
            assertFalse(logged.contains(SENSITIVE));
        } finally {
            logger.detachAppender(appender);
        }
    }

    private ListAppender<ILoggingEvent> attach(Logger logger) {
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        return appender;
    }
}
