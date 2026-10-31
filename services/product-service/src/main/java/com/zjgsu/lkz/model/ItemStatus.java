package com.zjgsu.lkz.model;

/** 商品状态：在售 → 被订单锁定 → 已售出。 */
public enum ItemStatus {
    ON_SALE,
    RESERVED,
    SOLD
}
