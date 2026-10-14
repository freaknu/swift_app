package com.swiftapp_backend.apigateway.gateway.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Map;
import java.net.URI;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class GatewayHostedDocumentationTest {
    @Autowired private ApplicationContext context;
    @Autowired private RouteDefinitionLocator routes;

    @Test
    void swaggerRedirectAndItsJavascriptAndStylesArePublic() {
        WebTestClient client = WebTestClient.bindToApplicationContext(context)
                .configureClient().baseUrl("https://swift-app.duckdns.org").build();
        String redirect = client.get().uri("/swagger-ui.html").exchange()
                .expectStatus().is3xxRedirection().returnResult(Void.class)
                .getResponseHeaders().getLocation().toString();
        assertTrue(redirect.endsWith("/swagger-ui/index.html"), redirect);
        client.get().uri(redirect).exchange().expectStatus().isOk()
                .expectBody(String.class).value(html -> assertTrue(html.contains("swagger-ui")));
        for (String asset : new String[]{"swagger-ui.css", "swagger-ui-bundle.js",
                "swagger-ui-standalone-preset.js", "swagger-initializer.js"}) {
            client.get().uri(URI.create(redirect).resolve(asset)).exchange().expectStatus().isOk();
        }
    }

    @Test
    void documentationConfigurationIsPublicAndIncludesEveryHttpService() {
        WebTestClient.bindToApplicationContext(context).build()
                .get().uri("/v3/api-docs/swagger-config")
                .exchange().expectStatus().isOk()
                .expectBody().jsonPath("$.urls.length()").isEqualTo(11)
                .jsonPath("$.urls[?(@.name == 'Chat Service')].url").isEqualTo("/chat/v3/api-docs")
                .jsonPath("$.urls[?(@.name == 'Notification Service')].url")
                .isEqualTo("/notification/v3/api-docs");
    }

    @Test
    void healthProbeIsPublicButDoesNotExposeRedisDetails() {
        WebTestClient.bindToApplicationContext(context).build()
                .get().uri("/actuator/health")
                .exchange().expectStatus().isEqualTo(503)
                .expectBody().jsonPath("$.status").isEqualTo("DOWN")
                .jsonPath("$.components").doesNotExist();
    }

    @Test
    void hostedOriginCanPreflightProtectedApiWithoutJwt() {
        WebTestClient.bindToApplicationContext(context).configureClient().baseUrl("http://localhost").build()
                .options().uri("/api/v1/ride/rides")
                .header("Origin", "https://swift-app.duckdns.org")
                .header("Access-Control-Request-Method", "GET")
                .header("Access-Control-Request-Headers", "Authorization")
                .exchange().expectStatus().isOk()
                .expectHeader().valueEquals("Access-Control-Allow-Origin", "https://swift-app.duckdns.org");
    }

    @Test
    void docsAndSocketRoutesRetainPublicGatewaySemantics() {
        Map<String, RouteDefinition> definitions = routes.getRouteDefinitions().collectList().block()
                .stream().collect(Collectors.toMap(RouteDefinition::getId, Function.identity()));
        var docs = definitions.values().stream().filter(route -> route.getId().endsWith("-openapi")).toList();
        assertEquals(11, docs.size());
        assertTrue(docs.stream().allMatch(route -> route.getFilters().stream()
                .anyMatch(filter -> filter.getName().equals("GatewayOpenApi"))));
        assertEquals("lb:ws://WEBSOCKET-SERVICE", definitions.get("websocket-native-service").getUri().toString());
        assertEquals("/ws-native,/ws-native/**", definitions.get("websocket-native-service")
                .getPredicates().getFirst().getArgs().get("_genkey_0") + ","
                + definitions.get("websocket-native-service").getPredicates().getFirst().getArgs().get("_genkey_1"));
        assertTrue(definitions.get("websocket-service").getPredicates().stream()
                .anyMatch(predicate -> predicate.getName().equals("Header")));
        assertEquals("lb://WEBSOCKET-SERVICE", definitions.get("websocket-sockjs-http").getUri().toString());
    }
}
