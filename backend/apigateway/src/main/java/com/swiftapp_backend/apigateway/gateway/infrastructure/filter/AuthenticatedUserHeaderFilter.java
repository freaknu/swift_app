package com.swiftapp_backend.apigateway.gateway.infrastructure.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.PathContainer;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.nio.charset.StandardCharsets;

@Component
public class AuthenticatedUserHeaderFilter implements GlobalFilter, Ordered {
    private static final String USER_ID_HEADER = "x-user-id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Internal decisions and account reviews must never be exposed through
        // the public gateway, even when the caller has a valid driver JWT.
        if (isInternalOnlyDecision(exchange)) {
            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
            return exchange.getResponse().setComplete();
        }
        if (isPublicRequest(exchange)) {
            return chain.filter(withoutClientUserId(exchange));
        }

        return exchange.getPrincipal()
                .ofType(JwtAuthenticationToken.class)
                .map(authentication -> userIdClaim(authentication))
                .defaultIfEmpty(-1L)
                .flatMap(userId -> {
                    if (userId <= 0) {
                        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                        return exchange.getResponse().setComplete();
                    }
                    return chain.filter(exchange.mutate()
                            .request(request -> request.headers(headers ->
                                    headers.set(USER_ID_HEADER, userId.toString())))
                            .build());
                });
    }

    private long userIdClaim(JwtAuthenticationToken authentication) {
        Object claim = authentication.getToken().getClaim("userId");
        if (claim instanceof Number number) {
            return number.longValue();
        }
        return -1L;
    }

    private ServerWebExchange withoutClientUserId(ServerWebExchange exchange) {
        return exchange.mutate()
                .request(request -> request.headers(headers -> headers.remove(USER_ID_HEADER)))
                .build();
    }

    private boolean isPublicRequest(ServerWebExchange exchange) {
        if (exchange.getRequest().getMethod() == HttpMethod.OPTIONS) {
            return true;
        }
        String path = exchange.getRequest().getPath().value();
        return (exchange.getRequest().getMethod() == HttpMethod.POST
                && path.equals("/api/v1/payments/webhook"))
                || under(path, "/api/v1/auth")
                || under(path, "/api/auth")
                || under(path, "/ws")
                || under(path, "/ws-native")
                || under(path, "/swagger-ui")
                || path.equals("/swagger-ui.html")
                || path.contains("/v3/api-docs");
    }

    private boolean under(String path, String prefix) {
        return path.equals(prefix) || path.startsWith(prefix + "/");
    }

    private boolean isInternalOnlyDecision(ServerWebExchange exchange) {
        var segments = normalizedSegments(exchange);
        if (segments.size() == 4
                && "api".equals(segments.get(0))
                && "v1".equals(segments.get(1))) {
            String service = segments.get(2);
            String action = segments.get(3);
            if (("document".equals(service) && "changeDocumentStatus".equals(action))
                    || ("vehicle".equals(service) && "verify".equals(action))
                    || ("driver".equals(service)
                    && ("suspend".equals(action) || "reactivate".equals(action)))) {
                return true;
            }
        }
        if (exchange.getRequest().getMethod() != HttpMethod.POST) return false;
        if (segments.size() != 6
                || !"api".equals(segments.get(0))
                || !"v1".equals(segments.get(1))) {
            return false;
        }
        boolean matchingDecision = "matching".equals(segments.get(2))
                && "offers".equals(segments.get(3))
                && ("accept".equals(segments.get(5)) || "reject".equals(segments.get(5)));
        boolean legacyRideAccept = "ride".equals(segments.get(2))
                && "rides".equals(segments.get(3))
                && "accept".equals(segments.get(5));
        boolean matchingCancellation = "matching".equals(segments.get(2))
                && "rides".equals(segments.get(3))
                && "cancel-pending".equals(segments.get(5));
        return matchingDecision || legacyRideAccept || matchingCancellation;
    }

    private List<String> normalizedSegments(ServerWebExchange exchange) {
        List<String> segments = new ArrayList<>();
        exchange.getRequest().getPath().pathWithinApplication()
                .elements().stream()
                .filter(PathContainer.PathSegment.class::isInstance)
                .map(PathContainer.PathSegment.class::cast)
                .map(PathContainer.PathSegment::valueToMatch)
                .forEach(segment -> {
                    // The downstream service decodes path segments again. Decode
                    // nested escapes here so a double encoded action cannot pass.
                    String decoded = segment;
                    for (int i = 0; i < 8 && decoded.contains("%"); i++) {
                        try {
                            String next = UriUtils.decode(decoded, StandardCharsets.UTF_8);
                            if (next.equals(decoded)) break;
                            decoded = next;
                        } catch (IllegalArgumentException malformed) {
                            break;
                        }
                    }
                    for (String part : decoded.split("/")) {
                        if (part.isEmpty() || ".".equals(part)) continue;
                        if ("..".equals(part)) {
                            if (!segments.isEmpty()) segments.remove(segments.size() - 1);
                        } else {
                            segments.add(part);
                        }
                    }
                });
        return segments;
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
