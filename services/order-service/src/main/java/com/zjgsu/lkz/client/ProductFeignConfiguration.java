package com.zjgsu.lkz.client;
import feign.Request;
import feign.Retryer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import java.util.concurrent.TimeUnit;
// Deliberately no @Configuration: these beans belong only to this Feign child context.
public class ProductFeignConfiguration {
    @Bean feign.RequestInterceptor bearerRelay() {
        return template->{
            var auth=org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if(auth instanceof org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken jwt)
                template.header("Authorization","Bearer "+jwt.getToken().getTokenValue());
        };
    }
    @Bean Retryer productRetryer() { return Retryer.NEVER_RETRY; }
    @Bean Request.Options productOptions(@Value("${clients.connect-timeout-ms}") int connect,
            @Value("${clients.read-timeout-ms}") int read) {
        return new Request.Options(connect,TimeUnit.MILLISECONDS,read,TimeUnit.MILLISECONDS,false);
    }
}
