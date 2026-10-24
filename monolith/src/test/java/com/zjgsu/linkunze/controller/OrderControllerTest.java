package com.zjgsu.linkunze.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.zjgsu.linkunze.exception.BusinessRuleException;
import com.zjgsu.linkunze.exception.ResourceNotFoundException;
import com.zjgsu.linkunze.model.OrderStatus;
import com.zjgsu.linkunze.model.TradeOrder;
import com.zjgsu.linkunze.service.OrderService;

/** 订单接口切片测试：下单 201、业务冲突 409、不存在 404、取消与成交。 */
@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    private TradeOrder sampleOrder() {
        return new TradeOrder("o1", "i1", "u-buyer",
                new BigDecimal("120.00"), OrderStatus.CREATED, LocalDateTime.of(2026, 10, 19, 12, 0));
    }

    @Test
    void placeOrderReturns201() throws Exception {
        when(orderService.placeOrder("i1", "u-buyer")).thenReturn(sampleOrder());

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemId\":\"i1\",\"buyerId\":\"u-buyer\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/orders/o1"))
                .andExpect(jsonPath("$.id").value("o1"))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.amount").value(120.0));
    }

    @Test
    void placeOrderOnSoldItemReturns409() throws Exception {
        when(orderService.placeOrder("i1", "u-buyer"))
                .thenThrow(new BusinessRuleException("商品当前状态不允许下单: SOLD"));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemId\":\"i1\",\"buyerId\":\"u-buyer\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void placeOrderWithUnknownItemReturns404() throws Exception {
        when(orderService.placeOrder("no-such", "u-buyer"))
                .thenThrow(new ResourceNotFoundException("商品不存在: no-such"));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemId\":\"no-such\",\"buyerId\":\"u-buyer\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("资源不存在"));
    }

    @Test
    void placeOrderWithBlankBuyerReturns400() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemId\":\"i1\",\"buyerId\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getOrderReturns200() throws Exception {
        when(orderService.getById("o1")).thenReturn(sampleOrder());

        mockMvc.perform(get("/api/orders/o1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemId").value("i1"))
                .andExpect(jsonPath("$.buyerId").value("u-buyer"));
    }

    @Test
    void cancelOrderReturns200() throws Exception {
        TradeOrder cancelled = new TradeOrder("o1", "i1", "u-buyer",
                new BigDecimal("120.00"), OrderStatus.CANCELLED, LocalDateTime.now());
        when(orderService.cancel("o1")).thenReturn(cancelled);

        mockMvc.perform(post("/api/orders/o1/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void completeOrderReturns200() throws Exception {
        TradeOrder completed = new TradeOrder("o1", "i1", "u-buyer",
                new BigDecimal("120.00"), OrderStatus.COMPLETED, LocalDateTime.now());
        when(orderService.complete("o1")).thenReturn(completed);

        mockMvc.perform(post("/api/orders/o1/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void listByBuyerReturns200() throws Exception {
        when(orderService.listByBuyer(anyString())).thenReturn(java.util.List.of(sampleOrder()));

        mockMvc.perform(get("/api/orders").param("buyerId", "u-buyer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("o1"));
    }
}
