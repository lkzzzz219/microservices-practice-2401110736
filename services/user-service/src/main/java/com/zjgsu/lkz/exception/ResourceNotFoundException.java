package com.zjgsu.lkz.exception;

/** 请求的资源不存在，映射 HTTP 404。 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
