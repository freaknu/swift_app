package com.swiftapp_backend.apigateway.gateway.infrastructure.filter;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.URI;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AuthenticatedUserHeaderFilterTest {
    private final AuthenticatedUserHeaderFilter filter = new AuthenticatedUserHeaderFilter();

    @Test
    void protectedRouteReplacesForgedUserIdWithSignedClaim() {
        MockServerWebExchange exchange = MockServerWebExchange.builder(
                        MockServerHttpRequest.get("/api/v1/driver/offers")
                                .header("x-user-id", "999"))
                .principal(principalWithUserId(42L))
                .build();
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

        StepVerifier.create(filter.filter(exchange, capture(forwarded))).verifyComplete();

        assertEquals("42", forwarded.get().getRequest().getHeaders().getFirst("x-user-id"));
    }

    @Test
    void publicAuthRouteDiscardsClientUserIdAndStillPassesThrough() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/v1/auth/login")
                        .header("x-user-id", "999"));
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

        StepVerifier.create(filter.filter(exchange, capture(forwarded))).verifyComplete();

        assertNull(forwarded.get().getRequest().getHeaders().getFirst("x-user-id"));
    }

    @Test
    void razorpayWebhookPassesWithoutJwtAndDiscardsForgedIdentity() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/v1/payments/webhook")
                        .header("x-user-id", "999")
                        .header("X-Razorpay-Signature", "signed-by-provider"));
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();
        StepVerifier.create(filter.filter(exchange, capture(forwarded))).verifyComplete();
        assertNull(forwarded.get().getRequest().getHeaders().getFirst("x-user-id"));
        assertEquals("signed-by-provider", forwarded.get().getRequest().getHeaders()
                .getFirst("X-Razorpay-Signature"));
    }

    @Test
    void anonymousPaymentActionsAndNonPostWebhookRemainProtected() {
        for (String path : new String[] {"/api/v1/payments/rides/15", "/api/v1/payments/webhook"}) {
            MockServerWebExchange exchange = MockServerWebExchange.from(
                    MockServerHttpRequest.get(path).header("x-user-id", "999"));
            AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();
            StepVerifier.create(filter.filter(exchange, capture(forwarded))).verifyComplete();
            assertNull(forwarded.get());
            assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        }
    }

    @Test
    void protectedRouteRejectsLegacyJwtWithoutNumericUserId() {
        MockServerWebExchange exchange = MockServerWebExchange.builder(
                        MockServerHttpRequest.get("/api/v1/driver/offers")
                                .header("x-user-id", "999"))
                .principal(principalWithoutUserId())
                .build();
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

        StepVerifier.create(filter.filter(exchange, capture(forwarded))).verifyComplete();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        assertNull(forwarded.get());
    }

    @Test
    void credentialRouteRequiresSignedIdentityAndReplacesForgedHeader() {
        MockServerWebExchange anonymous = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/user-cred/getCredentials/42")
                        .header("x-user-id", "42"));
        AtomicReference<ServerWebExchange> anonymousForwarded = new AtomicReference<>();
        StepVerifier.create(filter.filter(anonymous, capture(anonymousForwarded))).verifyComplete();
        assertEquals(HttpStatus.UNAUTHORIZED, anonymous.getResponse().getStatusCode());
        assertNull(anonymousForwarded.get());

        MockServerWebExchange authenticated = MockServerWebExchange.builder(
                        MockServerHttpRequest.get("/api/v1/user-cred/getCredentials/42")
                                .header("x-user-id", "999"))
                .principal(principalWithUserId(42L)).build();
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();
        StepVerifier.create(filter.filter(authenticated, capture(forwarded))).verifyComplete();
        assertEquals("42", forwarded.get().getRequest().getHeaders().getFirst("x-user-id"));
    }

    @Test
    void authenticatedCallerCannotBypassOfferOwnershipThroughMatchingRoute() {
        for (String decision : new String[] {"accept", "reject", "%61ccept"}) {
            MockServerWebExchange exchange = MockServerWebExchange.builder(
                            MockServerHttpRequest.method(HttpMethod.POST, URI.create(
                                    "http://localhost/api/v1/matching/offers/15/" + decision + "?driverId=999")))
                    .principal(principalWithUserId(42L))
                    .build();
            AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

            StepVerifier.create(filter.filter(exchange, capture(forwarded))).verifyComplete();

            assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
            assertNull(forwarded.get());
        }
    }

    @Test
    void authenticatedCallerCannotCancelPendingMatchThroughInternalRoute() {
        for (String action : new String[] {"cancel-pending", "%63ancel-pending"}) {
            MockServerWebExchange exchange = MockServerWebExchange.builder(
                            MockServerHttpRequest.method(HttpMethod.POST, URI.create(
                                    "http://localhost/api/v1/matching/rides/15/" + action)))
                    .principal(principalWithUserId(42L))
                    .build();
            AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

            StepVerifier.create(filter.filter(exchange, capture(forwarded))).verifyComplete();

            assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
            assertNull(forwarded.get());
        }
    }

    @Test
    void authenticatedCallerCannotAcceptRideThroughLegacyRiderRoute() {
        for (String action : new String[] {"accept", "%61ccept"}) {
            MockServerWebExchange exchange = MockServerWebExchange.builder(
                            MockServerHttpRequest.method(HttpMethod.POST, URI.create(
                                    "http://localhost/api/v1/ride/rides/15/" + action)))
                    .principal(principalWithUserId(42L))
                    .build();
            AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

            StepVerifier.create(filter.filter(exchange, capture(forwarded))).verifyComplete();

            assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
            assertNull(forwarded.get());
        }
    }

    @Test
    void authenticatedCallerCannotUseDriverReviewOrAccountAdministration() {
        for (String path : new String[] {
                "/api/v1/document/changeDocumentStatus",
                "/api/v1/document/%63hangeDocumentStatus/",
                "/api/v1/document/%2563hangeDocumentStatus/",
                "/api/v1/vehicle/verify",
                "/api/v1/vehicle/%76erify/",
                "/api/v1/vehicle/%2576erify/",
                "/api/v1/driver/suspend",
                "/api/v1/driver/%73uspend/",
                "/api/v1/driver/reactivate",
                "/api/v1/driver/%72eactivate/",
                "/api/v1/driver/%2572eactivate/",
                "/api/v1/driver/./reactivate",
                "/api/v1/driver/status/../reactivate"
        }) {
            MockServerWebExchange exchange = MockServerWebExchange.builder(
                            MockServerHttpRequest.method(HttpMethod.PUT,
                                    URI.create("http://localhost" + path)))
                    .principal(principalWithUserId(42L))
                    .build();
            AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

            StepVerifier.create(filter.filter(exchange, capture(forwarded))).verifyComplete();

            assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode(), path);
            assertNull(forwarded.get(), path);
        }
    }

    private GatewayFilterChain capture(AtomicReference<ServerWebExchange> forwarded) {
        return exchange -> {
            forwarded.set(exchange);
            return Mono.empty();
        };
    }

    private JwtAuthenticationToken principalWithUserId(Long userId) {
        Jwt jwt = Jwt.withTokenValue("signed-access-token")
                .header("alg", "HS256")
                .subject("driver@example.com")
                .claim("userId", userId)
                .build();
        return new JwtAuthenticationToken(jwt);
    }

    private JwtAuthenticationToken principalWithoutUserId() {
        Jwt jwt = Jwt.withTokenValue("legacy-access-token")
                .header("alg", "HS256")
                .subject("driver@example.com")
                .build();
        return new JwtAuthenticationToken(jwt);
    }
}
