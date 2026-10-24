package com.zjgsu.linkunze.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import com.zjgsu.linkunze.model.Item;
import com.zjgsu.linkunze.model.ItemStatus;

/** 商品仓储测试：验证实体保存、按主键查询与按状态查询。 */
@DataJpaTest
class ItemRepositoryTest {

    @Autowired
    private ItemRepository itemRepository;

    private Item sampleItem(String id, String sellerId, ItemStatus status) {
        return new Item(id, sellerId, "九成新自行车", "毕业转让", new BigDecimal("120.00"),
                status, LocalDateTime.now());
    }

    @Test
    void saveAndFindById() {
        itemRepository.save(sampleItem("i1", "u-seller", ItemStatus.ON_SALE));

        Item loaded = itemRepository.findById("i1").orElseThrow();
        assertEquals("九成新自行车", loaded.getTitle());
        assertEquals(new BigDecimal("120.00"), loaded.getPrice());
        assertEquals(ItemStatus.ON_SALE, loaded.getStatus());
        assertEquals("u-seller", loaded.getSellerId());
    }

    @Test
    void findByStatusOnlyReturnsMatching() {
        itemRepository.save(sampleItem("i1", "u-seller", ItemStatus.ON_SALE));
        itemRepository.save(sampleItem("i2", "u-seller", ItemStatus.RESERVED));
        itemRepository.save(sampleItem("i3", "u-seller", ItemStatus.SOLD));

        assertEquals(1, itemRepository.findByStatus(ItemStatus.ON_SALE).size());
        assertEquals(1, itemRepository.findByStatus(ItemStatus.RESERVED).size());
        assertEquals(1, itemRepository.findByStatus(ItemStatus.SOLD).size());
        assertTrue(itemRepository.findByStatus(ItemStatus.ON_SALE).get(0).getId().equals("i1"));
    }
}
