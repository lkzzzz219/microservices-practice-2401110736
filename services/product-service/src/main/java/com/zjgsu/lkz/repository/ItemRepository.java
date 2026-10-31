package com.zjgsu.lkz.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zjgsu.lkz.model.Item;
import com.zjgsu.lkz.model.ItemStatus;

/** 商品仓储。 */
public interface ItemRepository extends JpaRepository<Item, String> {

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select i from Item i where i.id = :id")
    java.util.Optional<Item> findForUpdate(@org.springframework.data.repository.query.Param("id") String id);

    /** 按状态查询，用于首页“在售商品列表”。 */
    List<Item> findByStatus(ItemStatus status);
}
