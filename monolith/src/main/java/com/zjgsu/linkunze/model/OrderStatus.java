package com.zjgsu.linkunze.model;

/** 订单状态：创建后可确认成交或取消，终态不可再变。 */
public enum OrderStatus {
    CREATED,
    COMPLETED,
    CANCELLED
}
