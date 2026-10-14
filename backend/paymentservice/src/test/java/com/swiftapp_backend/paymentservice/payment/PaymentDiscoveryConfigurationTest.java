package com.swiftapp_backend.paymentservice.payment;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.cloud.commons.util.InetUtils;
import org.springframework.cloud.commons.util.InetUtilsProperties;
import org.springframework.cloud.netflix.eureka.EurekaInstanceConfigBean;
import org.springframework.core.io.support.ResourcePropertySource;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PaymentDiscoveryConfigurationTest {
    @Test
    void gatewayUsesTheLoopbackAddressWhereTheLocalPaymentServiceListens() throws Exception {
        MockEnvironment environment = new MockEnvironment();
        environment.getPropertySources().addLast(new ResourcePropertySource("classpath:application.properties"));
        try (InetUtils inet = new InetUtils(new InetUtilsProperties())) {
            EurekaInstanceConfigBean registration = new EurekaInstanceConfigBean(inet);
            // Simulate automatic selection of the development computer's LAN address.
            registration.setIpAddress("192.0.2.10");
            Binder.get(environment).bind("eureka.instance", Bindable.ofInstance(registration));

            assertEquals("127.0.0.1", environment.getProperty("server.address"));
            assertEquals(environment.getProperty("server.address"), registration.getHostName(false));
        }
    }

    @Test
    void explicitAdvertisementOverrideStillWorksForOtherDeploymentAddresses() throws Exception {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("SERVICE_BIND_ADDRESS", "0.0.0.0")
                .withProperty("SERVICE_ADVERTISE_ADDRESS", "192.0.2.20");
        environment.getPropertySources().addLast(new ResourcePropertySource("classpath:application.properties"));
        try (InetUtils inet = new InetUtils(new InetUtilsProperties())) {
            EurekaInstanceConfigBean registration = new EurekaInstanceConfigBean(inet);
            Binder.get(environment).bind("eureka.instance", Bindable.ofInstance(registration));

            assertEquals("0.0.0.0", environment.getProperty("server.address"));
            assertEquals("192.0.2.20", registration.getHostName(false));
        }
    }
}
