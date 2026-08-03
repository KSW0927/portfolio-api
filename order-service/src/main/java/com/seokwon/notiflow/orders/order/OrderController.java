package com.seokwon.notiflow.orders.order;

import org.springframework.http.ResponseEntity;
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
import com.seokwon.notiflow.orders.order.dto.OrderRequestDTO;
import com.seokwon.notiflow.orders.order.dto.OrderResultDTO;
import com.seokwon.notiflow.orders.order.dto.StockIntegrityRequestDTO;
import com.seokwon.notiflow.orders.lock.LockStrategy;
import com.seokwon.notiflow.orders.lock.DistributedLockService;
import com.seokwon.notiflow.orders.order.OrderService;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "주문 API", description = "재고 동시성 테스트용 주문 처리")
public class OrderController {

    private final OrderService orderService;
    private final DistributedLockService distributedLockService;

    @PostMapping
    @Operation(summary = "주문 생성", description = "상품 재고를 1개 차감. lockStrategy(기본 PESSIMISTIC)로 NONE/PESSIMISTIC/DISTRIBUTED 중 동시성 제어 방식을 고를 수 있음. NONE은 락 없이 처리해 동시성 이슈(오버셀)를 재현하고, DISTRIBUTED는 Redisson 분산락으로 직렬화함. 재고 부족 시에도 200으로 응답하며 결과에 성공 여부가 담김. 인증된 세션에서만 호출 가능하며, 실제 주문 소유자는 body의 buyerUserNo(테스트 구매자 풀)로 기록됨")
    public ResponseEntity<ApiResponse<OrderResultDTO>> placeOrder(@RequestBody @Valid OrderRequestDTO dto) {
        LockStrategy lockStrategy = dto.getLockStrategy() != null ? dto.getLockStrategy() : LockStrategy.PESSIMISTIC;

        // DISTRIBUTED는 OrderService.placeOrder(트랜잭션 메서드)를 시작하기 전에 분산락을 먼저 잡아야 한다.
        // 반드시 이 컨트롤러처럼 별도 빈(distributedLockService)이 Supplier로 orderService를 감싸서
        // "프록시를 거쳐" 호출해야 락 해제 시점에 DB 커밋까지 끝난 상태가 보장된다.
        OrderResultDTO result = lockStrategy == LockStrategy.DISTRIBUTED
                ? distributedLockService.executeWithLock(
                        "stock-lock:" + dto.getProductDetailId(),
                        () -> orderService.placeOrder(dto.getProductDetailId(), dto.getBuyerUserNo(), lockStrategy))
                : orderService.placeOrder(dto.getProductDetailId(), dto.getBuyerUserNo(), lockStrategy);

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

    @PostMapping("/batch-result")
    @Operation(summary = "배치 재고 정합성 결과 보고", description = "프론트가 시뮬레이션 1회 종료 후 계산한 예상/실제 재고를 Kafka로 발행. 오버셀(lostUnits > 0) 발생 시 notify-service가 우선순위 알림으로 저장/전파함")
    public ResponseEntity<ApiResponse<Void>> reportBatchResult(@RequestBody @Valid StockIntegrityRequestDTO dto) {

        System.out.println("dto >>>>> " + dto);

        orderService.reportBatchResult(dto);
        ApiResponse<Void> response = new ApiResponse<>(ResponseResult.SUCCESS_UPDATE, null);
        return ResponseEntity.status(response.getCode()).body(response);
    }
}
