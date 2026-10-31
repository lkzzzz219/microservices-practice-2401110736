package com.zjgsu.lkz.controller;
import com.zjgsu.lkz.model.Item;
import com.zjgsu.lkz.service.ItemService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/items/{id}")
public class ReservationController {
    private final ItemService items;
    public ReservationController(ItemService items) { this.items=items; }
    public record Reserve(@NotBlank String buyerId, @NotBlank String orderId) {}
    public record Finish(@NotBlank String orderId) {}
    @PostMapping("/reserve") public Item reserve(@PathVariable String id,@Valid @RequestBody Reserve r) { return items.reserve(id,r.buyerId(),r.orderId()); }
    @PostMapping("/release") public Item release(@PathVariable String id,@Valid @RequestBody Finish r) { return items.finish(id,r.orderId(),false); }
    @PostMapping("/sell") public Item sell(@PathVariable String id,@Valid @RequestBody Finish r) { return items.finish(id,r.orderId(),true); }
}
