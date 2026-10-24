package com.zjgsu.linkunze.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zjgsu.linkunze.model.Item;
import com.zjgsu.linkunze.model.ItemStatus;

/** 商品仓储。 */
public interface ItemRepository extends JpaRepository<Item, String> {

    /** 按状态查询，用于首页“在售商品列表”。 */
    List<Item> findByStatus(ItemStatus status);
}
