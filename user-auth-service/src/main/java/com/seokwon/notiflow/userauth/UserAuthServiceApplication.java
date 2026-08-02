package com.seokwon.notiflow.userauth;

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
public class UserAuthServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(UserAuthServiceApplication.class, args);
	}
}
