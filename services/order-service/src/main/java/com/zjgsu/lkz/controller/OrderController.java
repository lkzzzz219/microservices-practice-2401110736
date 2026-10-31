package com.zjgsu.lkz.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.zjgsu.lkz.dto.OrderDtos.CreateOrderRequest;
import com.zjgsu.lkz.dto.OrderDtos.OrderResponse;
import com.zjgsu.lkz.model.TradeOrder;
import com.zjgsu.lkz.service.OrderService;

import jakarta.validation.Valid;

/** 订单接口：下单、取消、确认成交与查询。 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /** 下单：商品必须存在、在售，且买家不是卖家。 */
    @PostMapping
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody CreateOrderRequest request) {
        TradeOrder order = orderService.placeOrder(request.itemId(), request.buyerId());
        return ResponseEntity
                .created(URI.create("/api/orders/" + order.getId()))
                .body(toResponse(order));
    }

    /** 订单详情。 */
    @GetMapping("/{id}")
    public OrderResponse getById(@PathVariable String id) {
        return toResponse(orderService.getById(id));
    }

    /** 某买家的订单列表。 */
    @GetMapping(params = "buyerId")
    public List<OrderResponse> listByBuyer(@RequestParam String buyerId) {
        return orderService.listByBuyer(buyerId).stream().map(this::toResponse).toList();
    }

    /** 取消订单，商品回到在售状态。 */
    @PostMapping("/{id}/cancel")
    public OrderResponse cancel(@PathVariable String id) {
        return toResponse(orderService.cancel(id));
    }

    /** 确认成交，商品标记为已售出。 */
    @PostMapping("/{id}/complete")
    public OrderResponse complete(@PathVariable String id) {
        return toResponse(orderService.complete(id));
    }

    private OrderResponse toResponse(TradeOrder order) {
        return new OrderResponse(order.getId(), order.getItemId(), order.getBuyerId(),
                order.getAmount(), order.getStatus(), order.getCreatedAt());
    }
}
