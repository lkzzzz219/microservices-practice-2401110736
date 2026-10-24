package com.zjgsu.linkunze.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.zjgsu.linkunze.exception.BusinessRuleException;
import com.zjgsu.linkunze.exception.ResourceNotFoundException;
import com.zjgsu.linkunze.model.Item;
import com.zjgsu.linkunze.model.ItemStatus;
import com.zjgsu.linkunze.model.OrderStatus;
import com.zjgsu.linkunze.model.TradeOrder;
import com.zjgsu.linkunze.repository.ItemRepository;
import com.zjgsu.linkunze.repository.OrderRepository;

/** 订单业务规则单元测试（纯 Mockito，不启动 Spring 容器）。 */
class OrderServiceTest {

    private OrderRepository orderRepository;
    private ItemRepository itemRepository;
    private OrderService orderService;

    private Item onSaleItem;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        itemRepository = mock(ItemRepository.class);
        orderService = new OrderService(orderRepository, itemRepository);

        onSaleItem = new Item("i1", "u-seller", "九成新自行车", "毕业转让",
                new BigDecimal("120.00"), ItemStatus.ON_SALE, LocalDateTime.now());
        when(itemRepository.findById("i1")).thenReturn(Optional.of(onSaleItem));
        when(orderRepository.save(any(TradeOrder.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(itemRepository.save(any(Item.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void placeOrderSuccessLocksItem() {
        TradeOrder order = orderService.placeOrder("i1", "u-buyer");

        assertEquals(OrderStatus.CREATED, order.getStatus());
        assertEquals(new BigDecimal("120.00"), order.getAmount()); // 价格快照
        assertEquals(ItemStatus.RESERVED, onSaleItem.getStatus());
        verify(orderRepository).save(any(TradeOrder.class));
        verify(itemRepository).save(onSaleItem);
    }

    @Test
    void placeOrderRejectedWhenItemNotFound() {
        assertThrows(ResourceNotFoundException.class,
                () -> orderService.placeOrder("no-such-item", "u-buyer"));
        verify(orderRepository, never()).save(any(TradeOrder.class));
    }

    @Test
    void placeOrderRejectedWhenItemNotOnSale() {
        Item reserved = new Item("i2", "u-seller", "书", null,
                new BigDecimal("10.00"), ItemStatus.RESERVED, LocalDateTime.now());
        when(itemRepository.findById("i2")).thenReturn(Optional.of(reserved));

        assertThrows(BusinessRuleException.class, () -> orderService.placeOrder("i2", "u-buyer"));
        assertEquals(ItemStatus.RESERVED, reserved.getStatus()); // 状态未被改动
        verify(orderRepository, never()).save(any(TradeOrder.class));
    }

    @Test
    void placeOrderRejectedWhenBuyingOwnItem() {
        assertThrows(BusinessRuleException.class,
                () -> orderService.placeOrder("i1", "u-seller")); // sellerId == buyerId
        assertEquals(ItemStatus.ON_SALE, onSaleItem.getStatus()); // 商品仍在售
        verify(orderRepository, never()).save(any(TradeOrder.class));
    }

    @Test
    void placeOrderRejectedWhenOpenOrderExists() {
        when(orderRepository.countByItemIdAndStatus("i1", OrderStatus.CREATED))
                .thenReturn(1L); // 已有进行中的订单

        assertThrows(BusinessRuleException.class, () -> orderService.placeOrder("i1", "u-buyer"));
        assertEquals(ItemStatus.ON_SALE, onSaleItem.getStatus());
        verify(orderRepository, never()).save(any(TradeOrder.class));
    }

    @Test
    void cancelRestoresItemOnSale() {
        TradeOrder order = orderService.placeOrder("i1", "u-buyer");
        String orderId = order.getId();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        orderService.cancel(orderId);

        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        assertEquals(ItemStatus.ON_SALE, onSaleItem.getStatus());
    }

    @Test
    void cancelRejectedWhenOrderFinished() {
        TradeOrder done = new TradeOrder("o9", "i1", "u-buyer",
                new BigDecimal("120.00"), OrderStatus.COMPLETED, LocalDateTime.now());
        when(orderRepository.findById("o9")).thenReturn(Optional.of(done));

        assertThrows(BusinessRuleException.class, () -> orderService.cancel("o9"));
        assertEquals(OrderStatus.COMPLETED, done.getStatus()); // 终态不可变
    }

    @Test
    void completeMarksItemSold() {
        TradeOrder order = orderService.placeOrder("i1", "u-buyer");
        String orderId = order.getId();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        orderService.complete(orderId);

        assertEquals(OrderStatus.COMPLETED, order.getStatus());
        assertEquals(ItemStatus.SOLD, onSaleItem.getStatus());
    }
}
