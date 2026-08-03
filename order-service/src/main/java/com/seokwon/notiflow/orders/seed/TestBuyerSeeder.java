package com.seokwon.notiflow.orders.seed;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import com.seokwon.notiflow.orders.customer.CustomerEntity;
import com.seokwon.notiflow.orders.customer.CustomerRepository;

/**
 * 동시 주문 시뮬레이션용 테스트 구매자(customers) 풀 시딩
 * @description 실제 user-auth-service 로그인 계정과는 별개의 로컬 데이터.
 * "100/500/1000명이 동시에 주문한 것"처럼 보이도록, 프론트에서 주문마다 이 풀 중
 * 하나를 랜덤으로 골라 buyerUserNo로 넘김. 요청 자체의 인증(누가 이 배치를 실행했는지)은
 * JWT로 따로 검증되고, 이 구매자 데이터는 "누구 앞으로 기록할지"만 담당함.
 */
@Component
@RequiredArgsConstructor
@Order(2)
public class TestBuyerSeeder implements CommandLineRunner {

    public static final int BUYER_POOL_SIZE = 2000;

    private final CustomerRepository customerRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (customerRepository.count() > 0) {
            return;
        }

        List<CustomerEntity> buyers = new ArrayList<>(BUYER_POOL_SIZE);
        for (long userNo = 1; userNo <= BUYER_POOL_SIZE; userNo++) {
            buyers.add(CustomerEntity.builder()
                    .userNo(userNo)
                    .username("테스트유저" + String.format("%04d", userNo))
                    .build());
        }

        customerRepository.saveAll(buyers);
    }
}
