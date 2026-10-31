package com.zjgsu.lkz.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.zjgsu.lkz.model.OrderStatus;

import jakarta.validation.constraints.NotBlank;

/** 订单相关 DTO。 */
public final class OrderDtos {

    private OrderDtos() {
    }

    /** 下单请求。 */
    public record CreateOrderRequest(
            @NotBlank String itemId,
            @NotBlank String buyerId) {
    }

    /** 订单响应。 */
    public record OrderResponse(
            String id,
            String itemId,
            String buyerId,
            BigDecimal amount,
            OrderStatus status,
            LocalDateTime createdAt) {
    }
}
