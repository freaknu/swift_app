package com.swiftapp_backend.paymentservice;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
@SpringBootApplication
@EnableScheduling
public class PaymentserviceApplication {
    public static void main(String[] args) { SpringApplication.run(PaymentserviceApplication.class, args); }
}
