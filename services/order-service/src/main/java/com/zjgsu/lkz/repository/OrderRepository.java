package com.zjgsu.lkz.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zjgsu.lkz.model.OrderStatus;
import com.zjgsu.lkz.model.TradeOrder;

/** 订单仓储。 */
public interface OrderRepository extends JpaRepository<TradeOrder, String> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select o from TradeOrder o where o.id = :id")
    java.util.Optional<TradeOrder> findForUpdate(@org.springframework.data.repository.query.Param("id") String id);


    /** 查询某买家名下的全部订单。 */
    List<TradeOrder> findByBuyerId(String buyerId);

    /** 查询某商品的全部订单（正常流程下一条，用于排查与审计）。 */
    List<TradeOrder> findByItemId(String itemId);

    /** 统计商品在“进行中”状态下的订单数，用于防止重复下单。 */
    long countByItemIdAndStatus(String itemId, OrderStatus status);
}
