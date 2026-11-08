package com.zjgsu.lkz;
import com.zjgsu.lkz.service.OrderService;
import com.zjgsu.lkz.repository.OrderRepository;
import com.zjgsu.lkz.model.OrderStatus;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.*;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
class OrderServiceTest {
    @Autowired OrderService orders;
    @Autowired OrderRepository repository;
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    org.springframework.cloud.client.discovery.DiscoveryClient discovery;
    private void available() {
        var uri=java.net.URI.create(DependencyFixture.url());
        org.mockito.Mockito.when(discovery.getInstances(org.mockito.ArgumentMatchers.anyString())).thenReturn(
            java.util.List.of(new org.springframework.cloud.client.DefaultServiceInstance("fixture", "fixture", uri.getHost(), uri.getPort(), false)));
    }
    @BeforeEach void reset() { available();
        repository.deleteAll(); DependencyFixture.status=200; DependencyFixture.delay=0;
        DependencyFixture.body="{\"id\":\"item-1\",\"sellerId\":\"seller\",\"price\":12.50,\"status\":\"RESERVED\"}";
    }
    @Test void emptyInstancesWriteNothing() {
        org.mockito.Mockito.when(discovery.getInstances("user-service")).thenReturn(java.util.List.of());
        var e=assertThrows(ResponseStatusException.class,()->orders.placeOrder("item-1","buyer"));
        assertEquals(503,e.getStatusCode().value()); assertEquals(0,repository.count());
    }
    @Test void emptyProductInstancesWriteNothing() {
        org.mockito.Mockito.when(discovery.getInstances("product-service")).thenReturn(java.util.List.of());
        var e=assertThrows(ResponseStatusException.class,()->orders.placeOrder("item-1","buyer"));
        assertEquals(503,e.getStatusCode().value()); assertEquals(0,repository.count());
    }
    @Test void eachOperationDiscoversAgain() {
        orders.placeOrder("item-1","buyer");
        org.mockito.Mockito.when(discovery.getInstances("product-service")).thenReturn(java.util.List.of());
        assertThrows(ResponseStatusException.class,()->orders.placeOrder("item-2","buyer"));
        assertEquals(1,repository.count());
    }
    @AfterAll static void close() { DependencyFixture.server.stop(0); }
    @Test void httpReservationThenLocalOrder() {
        var o=orders.placeOrder("item-1","buyer");
        assertEquals(1,repository.count()); assertEquals("12.50",o.getAmount().toPlainString());
        assertEquals(OrderStatus.COMPLETED,orders.complete(o.getId()).getStatus());
    }
    @Test void missingResourceWritesNothing() { failWith(404,404); }
    @Test void dependencyDownWritesNothing() { failWith(503,503); }
    @Test void conflictWritesNothing() { failWith(409,409); }
    @Test void readTimeoutWritesNothing() { DependencyFixture.delay=600; failWith(200,503); }
    private void failWith(int remote,int expected) {
        DependencyFixture.status=remote;
        var e=assertThrows(ResponseStatusException.class,()->orders.placeOrder("item-1","buyer"));
        assertEquals(expected,e.getStatusCode().value()); assertEquals(0,repository.count());
    }
}
