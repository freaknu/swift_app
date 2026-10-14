package com.swiftapp_backend.apigateway.gateway.infrastructure.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

@Configuration
public class GatewayRateLimitConfig {
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> exchange.getRequest().getHeaders().getFirst("x-user-id") != null
                ? Mono.just(exchange.getRequest().getHeaders().getFirst("x-user-id"))
                : exchange.getPrincipal().map(principal -> principal.getName())
                .switchIfEmpty(Mono.justOrEmpty(exchange.getRequest().getRemoteAddress())
                        // ForwardedHeaderTransformer deliberately avoids DNS
                        // and supplies an unresolved address behind Nginx.
                        .map(address -> address.getHostString()))
                .defaultIfEmpty("anonymous");
    }

    @Bean
    public RedisRateLimiter redisRateLimiter() {
        return new RedisRateLimiter(20, 40);
    }
}
