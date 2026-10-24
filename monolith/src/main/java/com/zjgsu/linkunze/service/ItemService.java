package com.zjgsu.linkunze.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zjgsu.linkunze.exception.ResourceNotFoundException;
import com.zjgsu.linkunze.model.Item;
import com.zjgsu.linkunze.model.ItemStatus;
import com.zjgsu.linkunze.repository.ItemRepository;

@Service
public class ItemService {

    private final ItemRepository itemRepository;

    public ItemService(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    /** 发布商品，初始状态为在售。 */
    @Transactional
    public Item create(String sellerId, String title, String description, java.math.BigDecimal price) {
        Item item = new Item(UUID.randomUUID().toString(), sellerId, title, description,
                price, ItemStatus.ON_SALE, LocalDateTime.now());
        return itemRepository.save(item);
    }

    /** 查询在售商品列表（首页）。 */
    @Transactional(readOnly = true)
    public List<Item> listOnSale() {
        return itemRepository.findByStatus(ItemStatus.ON_SALE);
    }

    /** 按 id 查询商品详情。 */
    @Transactional(readOnly = true)
    public Item getById(String id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("商品不存在: " + id));
    }
}
