package com.swiftapp_backend.driverservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableKafka
@EnableAsync
public class DriverserviceApplication {

	public static void main(String[] args) {
		SpringApplication.run(DriverserviceApplication.class, args);
	}

}
