package com.seokwon.notiflow.orders.product;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import com.seokwon.notiflow.orders.product.dto.ProductDetailDTO;
import com.seokwon.notiflow.orders.product.ProductDetailRepository;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductDetailRepository productDetailRepository;

    @Transactional(readOnly = true)
    public List<ProductDetailDTO> getAllProducts() {
        return productDetailRepository.findAllWithProduct().stream()
                .map(ProductDetailDTO::from)
                .toList();
    }
}
