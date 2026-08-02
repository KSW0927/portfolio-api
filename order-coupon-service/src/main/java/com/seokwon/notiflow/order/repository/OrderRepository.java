package com.seokwon.notiflow.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.seokwon.notiflow.order.entity.OrderEntity;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
}
