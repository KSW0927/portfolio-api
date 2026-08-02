package com.seokwon.notiflow.order.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import com.seokwon.notiflow.order.dto.ProductDetailDTO;
import com.seokwon.notiflow.order.repository.ProductDetailRepository;

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
