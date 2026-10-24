package com.zjgsu.linkunze.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zjgsu.linkunze.model.OrderStatus;
import com.zjgsu.linkunze.model.TradeOrder;

/** 订单仓储。 */
public interface OrderRepository extends JpaRepository<TradeOrder, String> {

    /** 查询某买家名下的全部订单。 */
    List<TradeOrder> findByBuyerId(String buyerId);

    /** 查询某商品的全部订单（正常流程下一条，用于排查与审计）。 */
    List<TradeOrder> findByItemId(String itemId);

    /** 统计商品在“进行中”状态下的订单数，用于防止重复下单。 */
    long countByItemIdAndStatus(String itemId, OrderStatus status);
}
