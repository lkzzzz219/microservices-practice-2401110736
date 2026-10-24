package com.zjgsu.linkunze.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zjgsu.linkunze.exception.BusinessRuleException;
import com.zjgsu.linkunze.exception.ResourceNotFoundException;
import com.zjgsu.linkunze.model.Item;
import com.zjgsu.linkunze.model.ItemStatus;
import com.zjgsu.linkunze.model.OrderStatus;
import com.zjgsu.linkunze.model.TradeOrder;
import com.zjgsu.linkunze.repository.ItemRepository;
import com.zjgsu.linkunze.repository.OrderRepository;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ItemRepository itemRepository;

    public OrderService(OrderRepository orderRepository, ItemRepository itemRepository) {
        this.orderRepository = orderRepository;
        this.itemRepository = itemRepository;
    }

    /**
     * 下单业务规则（顺序执行，任一不满足即拒绝，不产生任何修改）：
     * 1. 商品必须存在；
     * 2. 商品必须处于在售状态（已被锁定或已售出的不能重复下单）；
     * 3. 买家不能购买自己发布的商品；
     * 4. 创建订单（金额取商品价格快照），商品状态改为已锁定。
     * 订单与商品状态在同一事务中写入，一起成功或一起回滚。
     */
    @Transactional
    public TradeOrder placeOrder(String itemId, String buyerId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("商品不存在: " + itemId));

        if (!item.isPurchasable()) {
            throw new BusinessRuleException("商品当前状态不允许下单: " + item.getStatus());
        }
        if (item.getSellerId().equals(buyerId)) {
            throw new BusinessRuleException("不能购买自己发布的商品");
        }
        if (orderRepository.countByItemIdAndStatus(itemId, OrderStatus.CREATED) > 0) {
            throw new BusinessRuleException("该商品已有进行中的订单");
        }

        TradeOrder order = new TradeOrder(
                UUID.randomUUID().toString(),
                item.getId(),
                buyerId,
                item.getPrice(),
                OrderStatus.CREATED,
                LocalDateTime.now());
        orderRepository.save(order);
        item.setStatus(ItemStatus.RESERVED);
        itemRepository.save(item);
        return order;
    }

    /**
     * 取消订单：只有进行中的订单可以取消；
     * 订单置为已取消，商品回到在售状态，两次写入同事务。
     */
    @Transactional
    public TradeOrder cancel(String orderId) {
        TradeOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("订单不存在: " + orderId));
        if (order.getStatus() != OrderStatus.CREATED) {
            throw new BusinessRuleException("订单已结束，不能取消: " + order.getStatus());
        }
        Item item = itemRepository.findById(order.getItemId())
                .orElseThrow(() -> new ResourceNotFoundException("商品不存在: " + order.getItemId()));

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
        item.setStatus(ItemStatus.ON_SALE);
        itemRepository.save(item);
        return order;
    }

    /**
     * 确认成交：买家线下验货后确认，订单完成，商品标记为已售出（终态）。
     */
    @Transactional
    public TradeOrder complete(String orderId) {
        TradeOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("订单不存在: " + orderId));
        if (order.getStatus() != OrderStatus.CREATED) {
            throw new BusinessRuleException("订单已结束，不能确认成交: " + order.getStatus());
        }
        Item item = itemRepository.findById(order.getItemId())
                .orElseThrow(() -> new ResourceNotFoundException("商品不存在: " + order.getItemId()));

        order.setStatus(OrderStatus.COMPLETED);
        orderRepository.save(order);
        item.setStatus(ItemStatus.SOLD);
        itemRepository.save(item);
        return order;
    }

    /** 按 id 查询订单详情。 */
    @Transactional(readOnly = true)
    public TradeOrder getById(String orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("订单不存在: " + orderId));
    }

    /** 查询某买家的全部订单。 */
    @Transactional(readOnly = true)
    public List<TradeOrder> listByBuyer(String buyerId) {
        return orderRepository.findByBuyerId(buyerId);
    }
}
