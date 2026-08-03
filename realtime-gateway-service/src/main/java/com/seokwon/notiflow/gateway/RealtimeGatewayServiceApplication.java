package com.seokwon.notiflow.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

import com.seokwon.notiflow.common.security.jwt.JwtTokenProvider;

/**
 * @description 이 서비스는 자체 인증 로직이 없어서(수신 전용) common의 CommonSecurityConfig는
 * 그대로 쓰지만(permitAll + CORS, 안 그러면 Spring Security 기본 Basic Auth가 걸림),
 * JwtTokenProvider는 여기서 실제로 쓰는 곳이 없으므로 스캔에서 제외함. 안 그러면 이 빈의
 * @PostConstruct가 jwt.secret 프로퍼티(및 .env 파일)를 요구하게 되어, DB/JWT와 무관해야 할
 * 이 서비스가 불필요하게 그 설정에 묶이게 됨.
 */
@SpringBootApplication
@ComponentScan(
        basePackages = "com.seokwon.notiflow",
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtTokenProvider.class)
)
public class RealtimeGatewayServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(RealtimeGatewayServiceApplication.class, args);
	}
}
