package com.seokwon.notiflow.orders.product.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import com.seokwon.notiflow.orders.product.ProductDetailEntity;

@Getter
@AllArgsConstructor
public class ProductDetailDTO {
    private Long detailId;
    private String model;
    private String storage;
    private String color;
    private Integer stock;

    public static ProductDetailDTO from(ProductDetailEntity entity) {
        return new ProductDetailDTO(
                entity.getDetailId(),
                entity.getProduct().getModel(),
                entity.getStorage(),
                entity.getColor(),
                entity.getStock()
        );
    }
}
