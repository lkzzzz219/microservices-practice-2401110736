package com.zjgsu.linkunze.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** 二手商品。price 必须大于 0；status 流转由 OrderService 的业务规则驱动。 */
@Entity
@Table(name = "items")
public class Item {

    @Id
    private String id;

    /** 发布者（卖家）用户 id。 */
    @Column(name = "seller_id", nullable = false)
    private String sellerId;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(length = 1000)
    private String description;

    /** 售价，保留两位小数，必须大于 0。 */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ItemStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Item() {
    }

    public Item(String id, String sellerId, String title, String description,
                BigDecimal price, ItemStatus status, LocalDateTime createdAt) {
        if (price == null || price.signum() <= 0) {
            throw new IllegalArgumentException("价格必须大于 0");
        }
        this.id = id;
        this.sellerId = sellerId;
        this.title = title;
        this.description = description;
        this.price = price;
        this.status = status;
        this.createdAt = createdAt;
    }

    /** 是否可被下单：只有在售商品可以被购买。 */
    public boolean isPurchasable() {
        return status == ItemStatus.ON_SALE;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSellerId() {
        return sellerId;
    }

    public void setSellerId(String sellerId) {
        this.sellerId = sellerId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        if (price == null || price.signum() <= 0) {
            throw new IllegalArgumentException("价格必须大于 0");
        }
        this.price = price;
    }

    public ItemStatus getStatus() {
        return status;
    }

    public void setStatus(ItemStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
