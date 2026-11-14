package com.zjgsu.lkz.client;
import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.zjgsu.lkz.service.OrderService.ItemSnapshot;
@FeignClient(name="product-service", configuration=ProductFeignConfiguration.class)
public interface ProductClient {
    @PostMapping("/api/items/{id}/reserve")
    ResponseEntity<ItemSnapshot> reserve(@PathVariable("id") String id, @RequestBody Map<String,String> request);
    @PostMapping("/api/items/{id}/release")
    ResponseEntity<Map<String,Object>> release(@PathVariable("id") String id, @RequestBody Map<String,String> request);
    @PostMapping("/api/items/{id}/sell")
    ResponseEntity<Map<String,Object>> sell(@PathVariable("id") String id, @RequestBody Map<String,String> request);
}
