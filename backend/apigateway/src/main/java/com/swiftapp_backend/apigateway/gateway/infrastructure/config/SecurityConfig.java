package com.swiftapp_backend.apigateway.gateway.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.reactive.CorsConfigurationSource;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http, CorsConfigurationSource corsConfigurationSource) {

        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)

                .authorizeExchange(exchange -> exchange
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .pathMatchers(HttpMethod.GET,
                                "/actuator/health", "/actuator/health/**").permitAll()
                        .pathMatchers(HttpMethod.POST, "/api/v1/payments/webhook").permitAll()
                        .pathMatchers(
                                "/api/auth/**",
                                "/api/v1/auth/**",
                                "/ws",
                                "/ws/**",
                                "/ws-native",
                                "/ws-native/**"
                        )
                        .permitAll()
                        .pathMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/*/v3/api-docs/**"
                        )
                        .permitAll()

                        .anyExchange()
                        .authenticated()
                )
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .oauth2ResourceServer(oauth2 ->
                        oauth2.jwt(jwt -> {})
                )

                .build();
    }
}
