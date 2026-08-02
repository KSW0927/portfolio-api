package com.seokwon.notiflow.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

import com.seokwon.notiflow.common.security.config.CommonSecurityConfig;

@SpringBootApplication
@ComponentScan(
        basePackages = "com.seokwon.notiflow",
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = CommonSecurityConfig.class)
)
public class OrderCouponServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(OrderCouponServiceApplication.class, args);
	}
}
