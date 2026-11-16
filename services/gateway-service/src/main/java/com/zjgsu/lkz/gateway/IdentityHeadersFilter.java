package com.zjgsu.lkz.gateway;
import org.springframework.cloud.gateway.filter.*;
import org.springframework.core.Ordered;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
@Component
public class IdentityHeadersFilter implements GlobalFilter, Ordered {
    @Override public int getOrder() { return -100; }
    @Override public Mono<Void> filter(ServerWebExchange exchange,GatewayFilterChain chain) {
        return exchange.getPrincipal().map(principal->{
            var builder=exchange.getRequest().mutate().headers(h->{
                // Remove alternate spellings as well; HttpHeaders is case-insensitive.
                h.remove("X-User-Id");h.remove("X-User-Roles");h.remove("X-Username");
                h.remove("X-Role");h.remove("X-Roles");h.remove("X-Authenticated-User");
                if(principal instanceof JwtAuthenticationToken jwt) {
                    h.set("X-User-Id",jwt.getToken().getSubject());
                    h.set("X-User-Roles",String.join(",",jwt.getToken().getClaimAsStringList("roles")));
                }
            });
            return exchange.mutate().request(builder.build()).build();
        }).defaultIfEmpty(exchange.mutate().request(exchange.getRequest().mutate().headers(h->{
            h.remove("X-User-Id");h.remove("X-User-Roles");h.remove("X-Username");
            h.remove("X-Role");h.remove("X-Roles");h.remove("X-Authenticated-User");
        }).build()).build()).flatMap(chain::filter);
    }
}
