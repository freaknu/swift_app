package com.swiftapp_backend.apigateway.gateway.infrastructure.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class LoginRequestFilter
        extends AbstractGatewayFilterFactory<LoginRequestFilter.Config> {

    public LoginRequestFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {

        return (exchange, chain) -> {

            log.info(
                    "Login request received: {} {}",
                    exchange.getRequest().getMethod(),
                    exchange.getRequest().getURI()
            );

            return chain.filter(exchange);
        };
    }

    public static class Config {
    }
}
