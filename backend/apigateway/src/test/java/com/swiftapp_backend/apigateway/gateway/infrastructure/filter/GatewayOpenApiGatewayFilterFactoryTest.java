package com.swiftapp_backend.apigateway.gateway.infrastructure.filter;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.factory.rewrite.ModifyResponseBodyGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class GatewayOpenApiGatewayFilterFactoryTest {
    private final JsonMapper json = JsonMapper.builder().build();
    private final GatewayOpenApiGatewayFilterFactory filter = new GatewayOpenApiGatewayFilterFactory(
            new ModifyResponseBodyGatewayFilterFactory(
                    ExchangeStrategies.withDefaults().messageReaders(), Set.of(), Set.of()), json);

    @Test
    void aggregatedSpecsUseSameOriginAndOfferBearerAuthentication() {
        var output = json.readTree(filter.rewriteSpecification("""
                {"openapi":"3.1.0","servers":[{"url":"http://private-service:8083"}],
                 "paths":{"/api/v1/ride/rides":{"get":{"summary":"List rides"}}}}
                """));

        assertEquals("/", output.at("/servers/0/url").asText());
        assertEquals(1, output.get("servers").size());
        assertEquals("bearer", output.at("/components/securitySchemes/gatewayBearer/scheme").asText());
        assertTrue(output.at("/security/0/gatewayBearer").isArray());
        assertEquals("List rides", output.at("/paths/~1api~1v1~1ride~1rides/get/summary").asText());
    }

    @Test
    void publicLoginAndProviderWebhookRemainExecutableWithoutJwt() {
        var output = json.readTree(filter.rewriteSpecification("""
                {"openapi":"3.1.0","paths":{
                  "/api/v1/auth/login":{"post":{}},
                  "/api/v1/payments/webhook":{"post":{}},
                  "/api/v1/payments/rides/1/order":{"post":{}}}}
                """));

        assertEquals(0, output.at("/paths/~1api~1v1~1auth~1login/post/security").size());
        assertEquals(0, output.at("/paths/~1api~1v1~1payments~1webhook/post/security").size());
        assertTrue(output.at("/paths/~1api~1v1~1payments~1rides~11~1order/post/security").isMissingNode());
    }

    @Test
    void existingSecurityAndSchemaComponentsArePreserved() {
        var output = json.readTree(filter.rewriteSpecification("""
                {"openapi":"3.1.0","security":[{"custom":[]}],
                 "components":{"schemas":{"Ride":{"type":"object"}},
                               "securitySchemes":{"custom":{"type":"apiKey"}}}}
                """));

        assertTrue(output.at("/security/0/custom").isArray());
        assertEquals("object", output.at("/components/schemas/Ride/type").asText());
        assertEquals("apiKey", output.at("/components/securitySchemes/custom/type").asText());
    }

    @Test
    void nonSpecificationResponsesArePreserved() {
        for (String body : new String[] {"", "Bad gateway", "{\"message\":\"Unavailable\"}"}) {
            assertEquals(body, filter.rewriteSpecification(body));
        }
    }

    @Test
    void responseFilterWritesRewrittenJsonAndUpdatesContentLength() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/rider/v3/api-docs"));
        exchange.getResponse().setStatusCode(HttpStatus.OK);
        byte[] body = "{\"openapi\":\"3.1.0\"}".getBytes(StandardCharsets.UTF_8);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        exchange.getResponse().getHeaders().setContentLength(body.length);

        StepVerifier.create(filter.apply(new GatewayOpenApiGatewayFilterFactory.Config())
                .filter(exchange, downstream -> downstream.getResponse().writeWith(Mono.just(
                        downstream.getResponse().bufferFactory().wrap(body))))).verifyComplete();

        String output = exchange.getResponse().getBodyAsString().block();
        assertEquals("/", json.readTree(output).at("/servers/0/url").asText());
        assertEquals(output.getBytes(StandardCharsets.UTF_8).length,
                exchange.getResponse().getHeaders().getContentLength());
    }

    @Test
    void upstreamErrorsAreNotRewritten() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/rider/v3/api-docs"));
        exchange.getResponse().setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
        byte[] body = "{\"openapi\":\"unexpected error payload\"}".getBytes(StandardCharsets.UTF_8);

        StepVerifier.create(filter.apply(new GatewayOpenApiGatewayFilterFactory.Config())
                .filter(exchange, downstream -> downstream.getResponse().writeWith(Mono.just(
                        downstream.getResponse().bufferFactory().wrap(body))))).verifyComplete();

        assertEquals(new String(body, StandardCharsets.UTF_8),
                exchange.getResponse().getBodyAsString().block());
    }
}
