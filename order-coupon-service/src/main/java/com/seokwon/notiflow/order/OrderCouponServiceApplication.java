package com.seokwon.notiflow.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan("com.seokwon.notiflow")
public class OrderCouponServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(OrderCouponServiceApplication.class, args);
	}
}
