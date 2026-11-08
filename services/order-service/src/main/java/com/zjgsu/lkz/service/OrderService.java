package com.zjgsu.lkz.service;
import com.zjgsu.lkz.client.RemoteApi;
import com.zjgsu.lkz.repository.OrderRepository;
import com.zjgsu.lkz.model.*;
import com.zjgsu.lkz.exception.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Service
public class OrderService {
    private final OrderRepository orders;
    private final RemoteApi api;
    public record ItemSnapshot(String id, String sellerId, BigDecimal price, String status) {}
    public OrderService(OrderRepository orders, RemoteApi api) {
        this.orders=orders; this.api=api;
    }
    /** Remote reservation commits first; the local transaction cannot roll it back. */
    @Transactional
    public TradeOrder placeOrder(String itemId, String buyerId) {
        api.get("user-service", "/api/users/"+buyerId, Map.class);
        String id=UUID.randomUUID().toString();
        ItemSnapshot item=api.post("product-service", "/api/items/"+itemId+"/reserve",
            Map.of("buyerId",buyerId,"orderId",id),ItemSnapshot.class);
        return orders.saveAndFlush(new TradeOrder(id,item.id(),buyerId,item.price(),OrderStatus.CREATED,LocalDateTime.now()));
    }
    @Transactional public TradeOrder cancel(String id) { return finish(id,false); }
    @Transactional public TradeOrder complete(String id) { return finish(id,true); }
    private TradeOrder finish(String id,boolean complete) {
        TradeOrder order=orders.findForUpdate(id).orElseThrow(()->new ResourceNotFoundException("订单不存在: "+id));
        if (order.getStatus()!=OrderStatus.CREATED) throw new BusinessRuleException("订单已结束");
        api.post("product-service", "/api/items/"+order.getItemId()+(complete ? "/sell" : "/release"),Map.of("orderId",id),Map.class);
        order.setStatus(complete ? OrderStatus.COMPLETED : OrderStatus.CANCELLED);
        return orders.saveAndFlush(order);
    }
    public TradeOrder getById(String id) { return orders.findById(id).orElseThrow(()->new ResourceNotFoundException("订单不存在: "+id)); }
    public List<TradeOrder> listByBuyer(String id) { return orders.findByBuyerId(id); }
}
