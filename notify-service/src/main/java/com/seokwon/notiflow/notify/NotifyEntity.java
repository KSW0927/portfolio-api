package com.seokwon.notiflow.notify;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 알림 이력
 * @description order-events 토픽으로 들어온 주문 이벤트를 사람이 읽을 수 있는 알림 문구로
 * 가공해서 저장한 것. 프론트 알림 위젯이 조회/구독하는 대상.
 */
@Entity
@Table(name = "notify")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotifyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long notifyId;

    @Column(nullable = false)
    private Long orderId;

    /** 주문한 테스트 구매자 번호 - 알림 상세 팝업("누가 몇시몇분몇초에 무엇을 주문했다")용 */
    @Column(nullable = false)
    private Long buyerUserNo;

    /** 우선 노출 알림인지(품절 주문 또는 오버셀 요약) - 프론트에서 이 알림을 목록 최상단에 정렬하는 데 씀 */
    @Column(nullable = false)
    private boolean priority;

    @Column(nullable = false)
    private String category;   // 주문 / 재고 / 동시성 / 시스템 등 - 프론트 ProfileWidget 카테고리와 매칭

    @Column(nullable = false, length = 500)
    private String message;

    @Column(nullable = false)
    private boolean isRead;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
