package com.seokwon.notiflow.orders.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson 클라이언트 설정
 * @description 분산락(LockStrategy.DISTRIBUTED) 전용. 다른 서비스에서 이미 쓰는 것과 동일한
 * Redis 인스턴스(spring.redis.host/port, docker-compose의 redis 컨테이너)를 그대로 재사용한다 -
 * Spring Boot 버전에 따라 RedisProperties 프리픽스가 달라지는 문제를 피하려고 직접 프로퍼티를 읽어서
 * Redisson Config를 명시적으로 구성했다(자동설정에 기대지 않음).
 */
@Configuration
public class RedissonConfig {

    @Value("${spring.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.redis.port:6379}")
    private int redisPort;

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer().setAddress("redis://" + redisHost + ":" + redisPort);
        return Redisson.create(config);
    }
}
