package com.seokwon.notiflow.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan("com.seokwon.notiflow")
public class RealtimeGatewayServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(RealtimeGatewayServiceApplication.class, args);
	}
}
