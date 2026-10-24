package com.zjgsu.linkunze.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.zjgsu.linkunze.model.ItemStatus;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 商品相关 DTO：请求/响应结构与模型解耦。 */
public final class ItemDtos {

    private ItemDtos() {
    }

    /** 发布商品请求。 */
    public record CreateItemRequest(
            @NotBlank String sellerId,
            @NotBlank @Size(max = 100) String title,
            @Size(max = 1000) String description,
            @NotNull @DecimalMin(value = "0.01", message = "价格必须大于 0") BigDecimal price) {
    }

    /** 商品响应。 */
    public record ItemResponse(
            String id,
            String sellerId,
            String title,
            String description,
            BigDecimal price,
            ItemStatus status,
            LocalDateTime createdAt) {
    }
}
