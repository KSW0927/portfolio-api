package com.seokwon.notiflow.orders.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * 결제 확정 타이머용 스레드풀
 * @description DB를 주기적으로 폴링하는 대신, 주문이 성공 처리되는 순간 정확한 지연시간(300ms~8s)만큼
 * 딱 한 번 실행될 타이머를 예약하는 방식으로 결제 확정을 처리한다(PaymentConfirmationService 참고).
 * 배치 하나에 최대 수천 건의 주문이 몰릴 수 있어 기본 단일 스레드 스케줄러로는 부족해서 풀 크기를 넉넉히 둠.
 */
@Configuration
public class SchedulingConfig {

    @Bean
    public TaskScheduler paymentConfirmTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(20);
        scheduler.setThreadNamePrefix("payment-confirm-");
        scheduler.initialize();
        return scheduler;
    }
}
