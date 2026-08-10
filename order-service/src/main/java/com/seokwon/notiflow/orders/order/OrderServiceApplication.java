package com.seokwon.notiflow.orders.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import com.seokwon.notiflow.common.security.config.CommonSecurityConfig;

@SpringBootApplication
@ComponentScan(
        basePackages = "com.seokwon.notiflow",
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = CommonSecurityConfig.class)
)
@EntityScan(basePackages = "com.seokwon.notiflow.orders")
@EnableJpaRepositories(basePackages = "com.seokwon.notiflow.orders")
public class OrderServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(OrderServiceApplication.class, args);
	}
}
