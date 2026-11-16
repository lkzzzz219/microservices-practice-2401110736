package com.zjgsu.lkz.gateway;
import java.security.*;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.*;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.security.web.server.savedrequest.NoOpServerRequestCache;
@Configuration
public class SecurityConfig {
    @Bean ReactiveJwtDecoder decoder(@Value("${JWT_PUBLIC_KEY}") String encoded) throws Exception {
        var key=(RSAPublicKey)KeyFactory.getInstance("RSA").generatePublic(
                new X509EncodedKeySpec(Base64.getDecoder().decode(encoded)));
        var decoder=NimbusReactiveJwtDecoder.withPublicKey(key).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer("campustrade-user-service"));
        return decoder;
    }
    @Bean SecurityWebFilterChain security(ServerHttpSecurity http) {
        var roles=new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName("roles");roles.setAuthorityPrefix("ROLE_");
        var converter=new ReactiveJwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new ReactiveJwtGrantedAuthoritiesConverterAdapter(roles));
        return http.csrf(c->c.disable()).httpBasic(c->c.disable()).formLogin(c->c.disable())
            .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
            .requestCache(c->c.requestCache(NoOpServerRequestCache.getInstance()))
            .authorizeExchange(a->a
                .pathMatchers("/actuator/health").permitAll()
                .pathMatchers(HttpMethod.POST,"/api/auth/login").permitAll()
                .pathMatchers("/api/admin/**").hasRole("ADMIN")
                .pathMatchers(HttpMethod.POST,"/api/users").hasRole("ADMIN")
                .pathMatchers("/api/items/*/reserve","/api/items/*/release","/api/items/*/sell").denyAll()
                .pathMatchers("/api/**").authenticated().anyExchange().denyAll())
            .oauth2ResourceServer(o->o.jwt(j->j.jwtAuthenticationConverter(converter))).build();
    }
}
