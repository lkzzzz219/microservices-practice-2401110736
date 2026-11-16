package com.zjgsu.lkz.security;
import java.security.*;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.*;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@ConditionalOnProperty(name="app.security.enabled",havingValue="true",matchIfMissing=true)
public class ApiSecurity {
    @Bean
    public RSAPublicKey publicKey(@Value("${JWT_PUBLIC_KEY}") String encoded) throws Exception {
        return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(
                new X509EncodedKeySpec(Base64.getDecoder().decode(encoded)));
    }
    @Bean
    public JwtDecoder jwtDecoder(RSAPublicKey key) {
        var decoder=NimbusJwtDecoder.withPublicKey(key).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer("campustrade-user-service"));
        return decoder;
    }
    @Bean
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        var roles=new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName("roles");roles.setAuthorityPrefix("ROLE_");
        var converter=new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(roles);
        return http.csrf(c->c.disable()).httpBasic(c->c.disable()).formLogin(c->c.disable())
            .sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a->a
                .requestMatchers("/actuator/health","/api/auth/login").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST,"/api/users").hasRole("ADMIN")
                .requestMatchers("/api/**").authenticated().anyRequest().denyAll())
            .oauth2ResourceServer(o->o.jwt(j->j.jwtAuthenticationConverter(converter))).build();
    }
}

