package com.seokwon.notiflow.orders.customer;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.seokwon.notiflow.orders.customer.CustomerEntity;

public interface CustomerRepository extends JpaRepository<CustomerEntity, Long> {
    Optional<CustomerEntity> findByUserNo(Long userNo);
}
