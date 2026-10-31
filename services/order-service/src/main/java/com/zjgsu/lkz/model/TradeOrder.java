package com.zjgsu.lkz.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 交易订单（表名 trade_orders，避免与 SQL 关键字 ORDER 冲突）。
 * 通过 itemId、buyerId 关联商品与买家，amount 为下单时刻的价格快照。
 */
@Entity
@Table(name = "trade_orders")
public class TradeOrder {

    @Id
    private String id;

    /** 商品 id，逻辑外键指向 items.id。 */
    @Column(name = "item_id", nullable = false)
    private String itemId;

    /** 买家用户 id。 */
    @Column(name = "buyer_id", nullable = false)
    private String buyerId;

    /** 成交金额快照，下单时从商品价格复制，不受后续改价影响。 */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public TradeOrder() {
    }

    public TradeOrder(String id, String itemId, String buyerId,
                      BigDecimal amount, OrderStatus status, LocalDateTime createdAt) {
        this.id = id;
        this.itemId = itemId;
        this.buyerId = buyerId;
        this.amount = amount;
        this.status = status;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public String getBuyerId() {
        return buyerId;
    }

    public void setBuyerId(String buyerId) {
        this.buyerId = buyerId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
