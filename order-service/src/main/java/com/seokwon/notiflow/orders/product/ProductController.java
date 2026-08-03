package com.seokwon.notiflow.orders.product;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import com.seokwon.notiflow.common.response.ApiResponse;
import com.seokwon.notiflow.common.response.ResponseResult;
import com.seokwon.notiflow.orders.product.dto.ProductDetailDTO;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "상품 API", description = "상품/옵션 조회")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    @Operation(summary = "제품 목록 조회", description = "프론트에서 랜덤 주문을 만들 때 사용할 상품 조합과 잔여 재고 목록")
    public ResponseEntity<ApiResponse<List<ProductDetailDTO>>> getProducts() {
        List<ProductDetailDTO> products = productService.getAllProducts();
        ApiResponse<List<ProductDetailDTO>> response = new ApiResponse<>(ResponseResult.SUCCESS_READ, products);
        return ResponseEntity.status(response.getCode()).body(response);
    }
}
