package com.seokwon.notiflow.orders.seed;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import com.seokwon.notiflow.orders.product.ProductDetailEntity;
import com.seokwon.notiflow.orders.product.ProductEntity;
import com.seokwon.notiflow.orders.product.ProductDetailRepository;
import com.seokwon.notiflow.orders.product.ProductRepository;

/**
 * 상수 상품 데이터 시딩
 * @description 모델(product) x 용량 x 색상(product_detail) 조합을 애플리케이션 기동 시 한 번만 생성.
 * 재고를 일부러 적게(20~60개) 잡아서, 대량 동시 주문 시 일부 조합이 실제로 품절되도록 함(동시성 검증용).
 */
@Component
@RequiredArgsConstructor
@Order(1)
public class ProductSeeder implements CommandLineRunner {

    private static final String[] MODELS = {"Galaxy Z Flip8", "Galaxy Z Fold8", "Galaxy Z Fold8 Ultra", "iPhone 17", "iPhone 17 Pro"};
    private static final String[] STORAGES = {"128GB", "256GB", "512GB"};
    private static final String[] COLORS = {"Black", "White", "Blue"};
    private static final int MIN_STOCK = 20;
    private static final int STOCK_RANGE = 60; // 20 ~ 60

    private final ProductRepository productRepository;
    private final ProductDetailRepository productDetailRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (productRepository.count() > 0) {
            return;
        }

        Random random = new Random();
        List<ProductDetailEntity> details = new ArrayList<>();

        for (String model : MODELS) {
            ProductEntity product = productRepository.save(
                    ProductEntity.builder().model(model).build()
            );

            for (String storage : STORAGES) {
                for (String color : COLORS) {
                    details.add(ProductDetailEntity.builder()
                            .product(product)
                            .storage(storage)
                            .color(color)
                            .stock(MIN_STOCK + random.nextInt(STOCK_RANGE))
                            .build());
                }
            }
        }

        productDetailRepository.saveAll(details);
    }
}
