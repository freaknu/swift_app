package com.swiftapp_backend.analyticalservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
public class AnalyticalserviceApplication {

	public static void main(String[] args) {
		SpringApplication.run(AnalyticalserviceApplication.class, args);
	}

}
