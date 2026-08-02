package com.seokwon.notiflow.order;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import com.seokwon.notiflow.common.response.ApiResponse;
import com.seokwon.notiflow.common.response.ResponseResult;
import com.seokwon.notiflow.order.dto.OrderRequestDTO;
import com.seokwon.notiflow.order.dto.OrderResultDTO;
import com.seokwon.notiflow.order.dto.ProductDetailDTO;
import com.seokwon.notiflow.order.service.OrderService;
import com.seokwon.notiflow.order.service.ProductService;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "주문 API", description = "재고 동시성 테스트용 주문 처리")
public class OrderController {

    private final OrderService orderService;
    private final ProductService productService;

    @GetMapping("/products")
    @Operation(summary = "제품 목록 조회", description = "프론트에서 랜덤 주문을 만들 때 사용할 상품 조합과 잔여 재고 목록")
    public ResponseEntity<ApiResponse<List<ProductDetailDTO>>> getProducts() {
        List<ProductDetailDTO> products = productService.getAllProducts();
        ApiResponse<List<ProductDetailDTO>> response = new ApiResponse<>(ResponseResult.SUCCESS_READ, products);
        return ResponseEntity.status(response.getCode()).body(response);
    }

    @PostMapping
    @Operation(summary = "주문 생성", description = "상품 재고를 1개 차감. useLock(기본 true)이 true면 Pessimistic Lock으로 순차 처리하고, false면 락 없이 처리해 동시성 이슈(오버셀)를 재현함. 재고 부족 시에도 200으로 응답하며 결과에 성공 여부가 담김. 인증된 세션에서만 호출 가능하며, 실제 주문 소유자는 body의 buyerUserNo(테스트 구매자 풀)로 기록됨")
    public ResponseEntity<ApiResponse<OrderResultDTO>> placeOrder(@RequestBody @Valid OrderRequestDTO dto) {
        boolean useLock = dto.getUseLock() == null || dto.getUseLock();
        OrderResultDTO result = orderService.placeOrder(dto.getProductDetailId(), dto.getBuyerUserNo(), useLock);
        ApiResponse<OrderResultDTO> response = new ApiResponse<>(ResponseResult.SUCCESS_ORDER, result);
        return ResponseEntity.status(response.getCode()).body(response);
    }

    @PostMapping("/reset")
    @Operation(summary = "재고/주문 초기화", description = "모든 상품 재고를 재랜덤 부여하고 주문 이력을 삭제")
    public ResponseEntity<ApiResponse<Void>> reset() {
        orderService.resetAll();
        ApiResponse<Void> response = new ApiResponse<>(ResponseResult.SUCCESS_UPDATE, null);
        return ResponseEntity.status(response.getCode()).body(response);
    }
}
