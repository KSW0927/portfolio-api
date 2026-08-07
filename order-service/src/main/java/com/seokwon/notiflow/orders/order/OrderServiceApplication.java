package com.seokwon.notiflow.orders.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import com.seokwon.notiflow.common.security.config.CommonSecurityConfig;

/**
 * 도메인별 패키지 분리(orders.order/product/customer/lock/...) 이후
 * 이 클래스가 orders.order 밑으로 한 단계 더 들어가면서, Spring Boot 기본값(메인 클래스
 * 패키지 기준 하위 스캔)만으로는 형제 패키지(orders.product, orders.customer 등)의
 * @Entity/Repository를 못 찾는다. @EntityScan/@EnableJpaRepositories로 orders 전체를
 * 명시적으로 지정해서 해결함(@ComponentScan은 별개 메커니즘이라 이걸 대신해주지 않음).
 */
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
