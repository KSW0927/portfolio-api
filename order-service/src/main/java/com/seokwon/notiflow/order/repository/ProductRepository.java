package com.seokwon.notiflow.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.seokwon.notiflow.order.entity.ProductEntity;

public interface ProductRepository extends JpaRepository<ProductEntity, Long> {
}
