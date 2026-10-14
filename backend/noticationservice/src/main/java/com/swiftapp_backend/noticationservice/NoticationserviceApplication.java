package com.swiftapp_backend.noticationservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync(proxyTargetClass = true)
@EnableKafka
public class NoticationserviceApplication {

	public static void main(String[] args) {
		SpringApplication.run(NoticationserviceApplication.class, args);
	}

}
