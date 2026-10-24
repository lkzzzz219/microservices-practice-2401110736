package com.zjgsu.linkunze.service;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.zjgsu.linkunze.exception.BusinessRuleException;
import com.zjgsu.linkunze.model.Item;
import com.zjgsu.linkunze.model.ItemStatus;
import com.zjgsu.linkunze.model.OrderStatus;
import com.zjgsu.linkunze.model.TradeOrder;
import com.zjgsu.linkunze.repository.ItemRepository;

/**
 * 订单业务集成测试：注入真实的 JPA Repository（H2 内存库），
 * 验证“下单 → 取消 → 再下单 → 成交”的完整流程与业务规则。
 * 每个测试在事务中执行、结束后回滚，彼此独立，可重复运行。
 */
@SpringBootTest
@Transactional
class OrderServiceIntegrationTest {

    @Autowired
    private OrderService orderService;
    @Autowired
    private ItemService itemService;
    @Autowired
    private ItemRepository itemRepository;

    private Item newOnSaleItem() {
        return itemService.create("u-seller", "九成新自行车", "毕业转让", new BigDecimal("120.00"));
    }

    @Test
    void fullTradeFlow() {
        Item item = newOnSaleItem();
        assertEquals(ItemStatus.ON_SALE, item.getStatus());

        // 下单：订单创建、商品锁定
        TradeOrder order = orderService.placeOrder(item.getId(), "u-buyer");
        assertEquals(OrderStatus.CREATED, order.getStatus());
        assertEquals(ItemStatus.RESERVED,
                itemRepository.findById(item.getId()).orElseThrow().getStatus());

        // 已锁定商品不能被其他人重复下单
        assertThrows(BusinessRuleException.class,
                () -> orderService.placeOrder(item.getId(), "u-buyer-2"));

        // 取消：订单取消、商品回到在售
        orderService.cancel(order.getId());
        assertEquals(OrderStatus.CANCELLED, orderService.getById(order.getId()).getStatus());
        assertEquals(ItemStatus.ON_SALE,
                itemRepository.findById(item.getId()).orElseThrow().getStatus());

        // 取消后可再次下单，确认成交后商品售出（终态）
        TradeOrder second = orderService.placeOrder(item.getId(), "u-buyer");
        orderService.complete(second.getId());
        assertEquals(OrderStatus.COMPLETED, orderService.getById(second.getId()).getStatus());
        assertEquals(ItemStatus.SOLD,
                itemRepository.findById(item.getId()).orElseThrow().getStatus());

        // 已售商品不能再下单；已完成订单不能取消
        assertThrows(BusinessRuleException.class,
                () -> orderService.placeOrder(item.getId(), "u-buyer-3"));
        assertThrows(BusinessRuleException.class, () -> orderService.cancel(second.getId()));
    }

    @Test
    void sellerCannotBuyOwnItem() {
        Item item = newOnSaleItem();
        assertThrows(BusinessRuleException.class,
                () -> orderService.placeOrder(item.getId(), "u-seller"));
        // 被拒绝后商品仍可被其他人购买
        TradeOrder order = orderService.placeOrder(item.getId(), "u-buyer");
        assertEquals(OrderStatus.CREATED, order.getStatus());
    }

    @Test
    void placeOrderWithUnknownItemReturns404Rule() {
        com.zjgsu.linkunze.exception.ResourceNotFoundException ex =
                assertThrows(com.zjgsu.linkunze.exception.ResourceNotFoundException.class,
                        () -> orderService.placeOrder("no-such-item", "u-buyer"));
        assertEquals("商品不存在: no-such-item", ex.getMessage());
    }
}
