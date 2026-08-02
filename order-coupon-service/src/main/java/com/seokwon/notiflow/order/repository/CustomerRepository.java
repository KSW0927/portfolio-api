package com.seokwon.notiflow.order.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.seokwon.notiflow.order.entity.CustomerEntity;

public interface CustomerRepository extends JpaRepository<CustomerEntity, Long> {
    Optional<CustomerEntity> findByUserNo(Long userNo);
}
