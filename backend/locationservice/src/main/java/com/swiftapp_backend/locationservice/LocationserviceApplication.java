package com.swiftapp_backend.locationservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class LocationserviceApplication {

	public static void main(String[] args) {
		SpringApplication.run(LocationserviceApplication.class, args);
	}

}
