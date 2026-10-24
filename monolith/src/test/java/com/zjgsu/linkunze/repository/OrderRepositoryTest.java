package com.zjgsu.linkunze.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import com.zjgsu.linkunze.model.Item;
import com.zjgsu.linkunze.model.ItemStatus;
import com.zjgsu.linkunze.model.OrderStatus;
import com.zjgsu.linkunze.model.TradeOrder;

/** 订单仓储测试：验证订单读写、按买家查询和进行中订单计数。 */
@DataJpaTest
class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private ItemRepository itemRepository;

    @BeforeEach
    void seedItem() {
        itemRepository.save(new Item("i1", "u-seller", "九成新自行车", "毕业转让",
                new BigDecimal("120.00"), ItemStatus.ON_SALE, LocalDateTime.now()));
    }

    @Test
    void saveAndFindByBuyerId() {
        orderRepository.save(new TradeOrder("o1", "i1", "u-buyer",
                new BigDecimal("120.00"), OrderStatus.CREATED, LocalDateTime.now()));

        assertEquals(1, orderRepository.findByBuyerId("u-buyer").size());
        assertEquals(0, orderRepository.findByBuyerId("nobody").size());
        TradeOrder loaded = orderRepository.findById("o1").orElseThrow();
        assertEquals(OrderStatus.CREATED, loaded.getStatus());
        assertEquals(new BigDecimal("120.00"), loaded.getAmount());
    }

    @Test
    void countByItemIdAndStatus() {
        orderRepository.save(new TradeOrder("o1", "i1", "u-buyer",
                new BigDecimal("120.00"), OrderStatus.CREATED, LocalDateTime.now()));
        orderRepository.save(new TradeOrder("o2", "i1", "u-buyer",
                new BigDecimal("120.00"), OrderStatus.CANCELLED, LocalDateTime.now()));

        assertEquals(1, orderRepository.countByItemIdAndStatus("i1", OrderStatus.CREATED));
        assertEquals(1, orderRepository.countByItemIdAndStatus("i1", OrderStatus.CANCELLED));
        assertEquals(0, orderRepository.countByItemIdAndStatus("i1", OrderStatus.COMPLETED));
    }
}
