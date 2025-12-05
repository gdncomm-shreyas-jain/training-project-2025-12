package com.example.cart;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@EnableMongoRepositories
@EnableCaching
@EnableFeignClients
@SpringBootApplication
public class CartApplication {

	public static void main(String[] args) {
		SpringApplication.run(CartApplication.class, args);
	}

}
