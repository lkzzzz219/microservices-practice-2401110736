package com.zjgsu.linkunze.exception;

/** 业务规则冲突（如商品已售出、不能购买自己的商品），映射 HTTP 409。 */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
