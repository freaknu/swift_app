package com.swiftapp_backend.apigateway.gateway.infrastructure.filter;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.rewrite.ModifyResponseBodyGatewayFilterFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

/** Keeps aggregated service documentation usable from the public gateway. */
@Component
public class GatewayOpenApiGatewayFilterFactory
        extends AbstractGatewayFilterFactory<GatewayOpenApiGatewayFilterFactory.Config> {
    private static final String BEARER_SCHEME = "gatewayBearer";
    private final ModifyResponseBodyGatewayFilterFactory responseBody;
    private final ObjectMapper objectMapper;

    public GatewayOpenApiGatewayFilterFactory(
            ModifyResponseBodyGatewayFilterFactory responseBody, ObjectMapper objectMapper) {
        super(Config.class);
        this.responseBody = responseBody;
        this.objectMapper = objectMapper;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return responseBody.apply(new ModifyResponseBodyGatewayFilterFactory.Config()
                .setRewriteFunction(String.class, String.class, (exchange, body) -> {
                    var status = exchange.getResponse().getStatusCode();
                    if (body == null || (status != null && !status.is2xxSuccessful())) {
                        return Mono.justOrEmpty(body);
                    }
                    return Mono.just(rewriteSpecification(body));
                }));
    }

    String rewriteSpecification(String body) {
        JsonNode parsed;
        try {
            parsed = objectMapper.readTree(body);
        } catch (RuntimeException malformed) {
            return body;
        }
        if (!(parsed instanceof ObjectNode document) || !document.has("openapi")) {
            return body;
        }

        // A root-relative server works on HTTPS in production and local HTTP.
        // Inferring it in each service exposes its private Eureka host/port.
        document.putArray("servers").addObject()
                .put("url", "/").put("description", "Swift public API gateway");
        ObjectNode components = object(document, "components");
        object(components, "securitySchemes").putObject(BEARER_SCHEME)
                .put("type", "http").put("scheme", "bearer").put("bearerFormat", "JWT");
        if (!document.has("security")) {
            document.putArray("security").addObject().putArray(BEARER_SCHEME);
        }

        JsonNode paths = document.get("paths");
        if (paths instanceof ObjectNode pathItems) {
            for (String path : pathItems.propertyNames()) {
                JsonNode item = pathItems.get(path);
                if (!(item instanceof ObjectNode operations)) continue;
                boolean publicAuth = path.equals("/api/v1/auth")
                        || path.startsWith("/api/v1/auth/")
                        || path.equals("/api/auth") || path.startsWith("/api/auth/");
                for (String method : operations.propertyNames()) {
                    if (operations.get(method) instanceof ObjectNode operation
                            && (publicAuth || (path.equals("/api/v1/payments/webhook")
                            && method.equals("post")))) {
                        operation.putArray("security");
                    }
                }
            }
        }
        return objectMapper.writeValueAsString(document);
    }

    private ObjectNode object(ObjectNode parent, String field) {
        return parent.get(field) instanceof ObjectNode existing
                ? existing : parent.putObject(field);
    }

    public static class Config {}
}
