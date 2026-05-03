package com.bigbites.notification_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class notificationServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(notificationServiceApplication.class, args);
	}

}
