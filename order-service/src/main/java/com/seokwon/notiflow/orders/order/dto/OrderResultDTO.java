package com.seokwon.notiflow.orders.order.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import com.seokwon.notiflow.orders.order.OrderStatus;

@Getter
@AllArgsConstructor
public class OrderResultDTO {
    private Long orderId;
    private Long detailId;
    private String model;
    private String storage;
    private String color;
    private Long buyerUserNo;
    private boolean success;
    private OrderStatus status;
}
