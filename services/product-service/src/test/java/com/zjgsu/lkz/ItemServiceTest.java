package com.zjgsu.lkz;
import com.zjgsu.lkz.service.ItemService;
import com.zjgsu.lkz.repository.ItemRepository;
import com.zjgsu.lkz.exception.BusinessRuleException;
import com.zjgsu.lkz.model.ItemStatus;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.*;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
class ItemServiceTest {
    @Autowired ItemService items;
    @Autowired ItemRepository repository;
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    org.springframework.cloud.client.discovery.DiscoveryClient discovery;
    private void available() {
        var uri=java.net.URI.create(DependencyFixture.url());
        org.mockito.Mockito.when(discovery.getInstances(org.mockito.ArgumentMatchers.anyString())).thenReturn(
            java.util.List.of(new org.springframework.cloud.client.DefaultServiceInstance("fixture", "fixture", uri.getHost(), uri.getPort(), false)));
    }
    @BeforeEach void reset() { available(); repository.deleteAll(); DependencyFixture.status=200; DependencyFixture.body="{}"; DependencyFixture.delay=0; }
    @Test void emptyInstancesWriteNothing() {
        org.mockito.Mockito.when(discovery.getInstances("user-service")).thenReturn(java.util.List.of());
        var e=assertThrows(ResponseStatusException.class,()->items.create("seller","Book","",BigDecimal.ONE));
        assertEquals(503,e.getStatusCode().value()); assertEquals(0,repository.count());
    }
    @AfterAll static void close() { DependencyFixture.server.stop(0); }
    @Test void reserveAndReleaseBelongToOrder() {
        var i=items.create("seller","Book","",new BigDecimal("10.00"));
        items.reserve(i.getId(),"buyer","order-1");
        assertThrows(BusinessRuleException.class,()->items.reserve(i.getId(),"buyer2","order-2"));
        assertThrows(BusinessRuleException.class,()->items.finish(i.getId(),"wrong-order",false));
        assertEquals(ItemStatus.ON_SALE,items.finish(i.getId(),"order-1",false).getStatus());
        items.reserve(i.getId(),"buyer","order-3");
        assertEquals(ItemStatus.SOLD,items.finish(i.getId(),"order-3",true).getStatus());
    }
    @Test void cannotBuyOwnItem() {
        var i=items.create("seller","Book","",BigDecimal.ONE);
        assertThrows(BusinessRuleException.class,()->items.reserve(i.getId(),"seller","order"));
        assertEquals(ItemStatus.ON_SALE,items.getById(i.getId()).getStatus());
    }
    @Test void missingSellerWritesNothing() {
        DependencyFixture.status=404;
        var e=assertThrows(ResponseStatusException.class,()->items.create("missing","Book","",BigDecimal.ONE));
        assertEquals(404,e.getStatusCode().value()); assertEquals(0,repository.count());
    }
}
