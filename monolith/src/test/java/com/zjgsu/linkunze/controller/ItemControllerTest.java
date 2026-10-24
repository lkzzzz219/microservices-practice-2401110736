package com.zjgsu.linkunze.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
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

import com.zjgsu.linkunze.exception.ResourceNotFoundException;
import com.zjgsu.linkunze.model.Item;
import com.zjgsu.linkunze.model.ItemStatus;
import com.zjgsu.linkunze.service.ItemService;

/** 商品接口切片测试：校验、状态码与响应字段。 */
@WebMvcTest(ItemController.class)
class ItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ItemService itemService;

    private Item sampleItem() {
        return new Item("i1", "u-seller", "九成新自行车", "毕业转让",
                new BigDecimal("120.00"), ItemStatus.ON_SALE, LocalDateTime.of(2026, 10, 19, 12, 0));
    }

    @Test
    void createReturns201WithLocation() throws Exception {
        when(itemService.create(anyString(), anyString(), anyString(), any(BigDecimal.class)))
                .thenReturn(sampleItem());

        mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sellerId":"u-seller","title":"九成新自行车",
                                 "description":"毕业转让","price":120.00}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/items/i1"))
                .andExpect(jsonPath("$.id").value("i1"))
                .andExpect(jsonPath("$.status").value("ON_SALE"))
                .andExpect(jsonPath("$.price").value(120.0));
    }

    @Test
    void createWithInvalidPriceReturns400() throws Exception {
        mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sellerId":"u-seller","title":"九成新自行车","price":-5}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("参数无效"));
    }

    @Test
    void listOnSaleReturns200() throws Exception {
        when(itemService.listOnSale()).thenReturn(List.of(sampleItem()));

        mockMvc.perform(get("/api/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").doesNotExist())
                .andExpect(jsonPath("$[0].title").value("九成新自行车"))
                .andExpect(jsonPath("$[0].status").value("ON_SALE"));
    }

    @Test
    void getUnknownItemReturns404() throws Exception {
        when(itemService.getById("no-such")).thenThrow(new ResourceNotFoundException("商品不存在: no-such"));

        mockMvc.perform(get("/api/items/no-such"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
