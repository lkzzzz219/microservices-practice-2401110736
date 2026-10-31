package com.zjgsu.lkz.service;
import com.zjgsu.lkz.client.RemoteApi;
import com.zjgsu.lkz.repository.ItemRepository;
import com.zjgsu.lkz.model.*;
import com.zjgsu.lkz.exception.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Service
public class ItemService {
    private final ItemRepository items;
    private final RemoteApi api;
    private final String userUrl;
    public ItemService(ItemRepository items, RemoteApi api, @Value("${clients.user-url}") String userUrl) {
        this.items=items; this.api=api; this.userUrl=userUrl;
    }
    public Item create(String seller, String title, String description, BigDecimal price) {
        api.get(userUrl+"/api/users/"+seller, Map.class);
        return items.save(new Item(UUID.randomUUID().toString(),seller,title,description,price,ItemStatus.ON_SALE,LocalDateTime.now()));
    }
    public List<Item> listOnSale() { return items.findByStatus(ItemStatus.ON_SALE); }
    public Item getById(String id) { return items.findById(id).orElseThrow(()->new ResourceNotFoundException("商品不存在: "+id)); }
    @Transactional
    public Item reserve(String id, String buyerId, String orderId) {
        Item item=items.findForUpdate(id).orElseThrow(()->new ResourceNotFoundException("商品不存在: "+id));
        if (item.getSellerId().equals(buyerId)) throw new BusinessRuleException("不能购买自己的商品");
        if (!item.isPurchasable()) throw new BusinessRuleException("商品已锁定或售出");
        item.setStatus(ItemStatus.RESERVED); item.setReservationId(orderId);
        return items.save(item);
    }
    @Transactional
    public Item finish(String id, String orderId, boolean complete) {
        Item item=items.findForUpdate(id).orElseThrow(()->new ResourceNotFoundException("商品不存在: "+id));
        if (item.getStatus()!=ItemStatus.RESERVED || !orderId.equals(item.getReservationId()))
            throw new BusinessRuleException("订单不拥有此商品锁");
        item.setStatus(complete ? ItemStatus.SOLD : ItemStatus.ON_SALE);
        if (!complete) item.setReservationId(null);
        return items.save(item);
    }
}
