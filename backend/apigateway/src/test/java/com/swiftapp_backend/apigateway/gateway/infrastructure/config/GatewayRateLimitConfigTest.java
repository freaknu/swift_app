package com.swiftapp_backend.apigateway.gateway.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.adapter.ForwardedHeaderTransformer;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.InetSocketAddress;
import java.security.Principal;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GatewayRateLimitConfigTest {
    private final KeyResolver resolver = new GatewayRateLimitConfig().userKeyResolver();

    @Test
    void usesVerifiedGatewayUserIdBeforeNetworkIdentity() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/")
                .header("x-user-id", "42"));
        StepVerifier.create(resolver.resolve(exchange)).expectNext("42").verifyComplete();
    }

    @Test
    void authenticatedPrincipalHasItsOwnRateLimitBucket() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"))
                .mutate().principal(Mono.just((Principal) () -> "rider@example.test")).build();
        StepVerifier.create(resolver.resolve(exchange)).expectNext("rider@example.test").verifyComplete();
    }

    @Test
    void supportsResolvedAndUnresolvedNetworkAddressesWithoutDns() {
        for (var address : new InetSocketAddress[]{new InetSocketAddress("127.0.0.1", 8080),
                InetSocketAddress.createUnresolved("203.0.113.10", 443)}) {
            var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/").remoteAddress(address));
            StepVerifier.create(resolver.resolve(exchange)).expectNext(address.getHostString()).verifyComplete();
        }
    }

    @Test
    void nginxForwardedClientAddressDoesNotBreakPublicRoutes() {
        var request = MockServerHttpRequest.get("http://127.0.0.1:8080/auth/v3/api-docs")
                .remoteAddress(new InetSocketAddress("127.0.0.1", 50000))
                .header("X-Forwarded-For", "203.0.113.10")
                .header("X-Forwarded-Host", "swift-app.duckdns.org")
                .header("X-Forwarded-Proto", "https")
                .header("X-Forwarded-Port", "443").build();
        var forwarded = new ForwardedHeaderTransformer().apply(request);
        assertTrue(forwarded.getRemoteAddress().isUnresolved());
        var exchange = MockServerWebExchange.from(request).mutate().request(forwarded).build();
        StepVerifier.create(resolver.resolve(exchange)).expectNext("203.0.113.10").verifyComplete();
    }

    @Test
    void missingNetworkAddressUsesAnonymousBucket() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));
        StepVerifier.create(resolver.resolve(exchange)).expectNext("anonymous").verifyComplete();
    }
}
