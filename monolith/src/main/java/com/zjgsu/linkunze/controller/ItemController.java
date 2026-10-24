package com.zjgsu.linkunze.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zjgsu.linkunze.dto.ItemDtos.CreateItemRequest;
import com.zjgsu.linkunze.dto.ItemDtos.ItemResponse;
import com.zjgsu.linkunze.model.Item;
import com.zjgsu.linkunze.service.ItemService;

import jakarta.validation.Valid;

/** 商品接口：发布与查询。Controller 只做参数接收与响应构造，业务在 Service。 */
@RestController
@RequestMapping("/api/items")
public class ItemController {

    private final ItemService itemService;

    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    /** 发布商品。 */
    @PostMapping
    public ResponseEntity<ItemResponse> create(@Valid @RequestBody CreateItemRequest request) {
        Item item = itemService.create(request.sellerId(), request.title(),
                request.description(), request.price());
        return ResponseEntity
                .created(URI.create("/api/items/" + item.getId()))
                .body(toResponse(item));
    }

    /** 在售商品列表。 */
    @GetMapping
    public List<ItemResponse> listOnSale() {
        return itemService.listOnSale().stream().map(this::toResponse).toList();
    }

    /** 商品详情。 */
    @GetMapping("/{id}")
    public ItemResponse getById(@PathVariable String id) {
        return toResponse(itemService.getById(id));
    }

    private ItemResponse toResponse(Item item) {
        return new ItemResponse(item.getId(), item.getSellerId(), item.getTitle(),
                item.getDescription(), item.getPrice(), item.getStatus(), item.getCreatedAt());
    }
}
