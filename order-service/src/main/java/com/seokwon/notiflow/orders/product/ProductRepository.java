package com.seokwon.notiflow.orders.product;

import org.springframework.data.jpa.repository.JpaRepository;

import com.seokwon.notiflow.orders.product.ProductEntity;

public interface ProductRepository extends JpaRepository<ProductEntity, Long> {
}
