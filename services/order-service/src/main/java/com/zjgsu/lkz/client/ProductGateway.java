package com.zjgsu.lkz.client;
import java.util.Map;
import java.util.function.Supplier;
import feign.FeignException;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.context.request.*;
import com.zjgsu.lkz.service.OrderService.ItemSnapshot;
@Component
public class ProductGateway {
    private final ProductClient client;
    public ProductGateway(ProductClient client) { this.client=client; }
    public ItemSnapshot reserve(String item,String buyer,String order) {
        return invoke(()->client.reserve(item,Map.of("buyerId",buyer,"orderId",order)));
    }
    public void finish(String item,String order,boolean complete) {
        invoke(()->complete?client.sell(item,Map.of("orderId",order)):client.release(item,Map.of("orderId",order)));
    }
    private <T> T invoke(Supplier<ResponseEntity<T>> call) {
        try {
            ResponseEntity<T> response=call.get();
            if (response.getBody()==null) throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Empty product response");
            String instance=response.getHeaders().getFirst("X-Instance-Id");
            var attrs=(ServletRequestAttributes)RequestContextHolder.getRequestAttributes();
            if(instance!=null && attrs!=null && attrs.getResponse()!=null)
                attrs.getResponse().setHeader("X-Product-Instance",instance);
            org.slf4j.LoggerFactory.getLogger(getClass()).info("FEIGN product-service instance={}",instance);
            return response.getBody();
        } catch (FeignException e) {
            if(e.status()==404) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Product resource not found");
            if(e.status()==409) throw new ResponseStatusException(HttpStatus.CONFLICT,"Product business conflict");
            if(e.status()<0 || e.status()>=500) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Product dependency unavailable or timed out; remote write outcome may be unknown",e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Product dependency rejected request",e);
        }
    }
}
