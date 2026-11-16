package com.zjgsu.lkz.auth;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.zjgsu.lkz.repository.UserRepository;
import com.zjgsu.lkz.model.TradeUser;
import java.security.*;
import java.security.interfaces.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import org.springframework.security.crypto.password.*;
import org.springframework.security.oauth2.jwt.*;

@Configuration
@ConditionalOnProperty(name="app.security.enabled",havingValue="true",matchIfMissing=true)
public class AuthConfiguration {
    @Bean PasswordEncoder passwordEncoder() {
        return new Pbkdf2PasswordEncoder("",16,600000,
                Pbkdf2PasswordEncoder.SecretKeyFactoryAlgorithm.PBKDF2WithHmacSHA256);
    }
    @Bean JwtEncoder jwtEncoder(RSAPublicKey publicKey,@Value("${JWT_PRIVATE_KEY}") String encoded) throws Exception {
        var privateKey=(RSAPrivateKey)KeyFactory.getInstance("RSA").generatePrivate(
                new PKCS8EncodedKeySpec(Base64.getDecoder().decode(encoded)));
        var rsa=new com.nimbusds.jose.jwk.RSAKey.Builder(publicKey).privateKey(privateKey).build();
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(rsa)));
    }
    @Bean CommandLineRunner seed(AuthAccounts accounts,UserRepository users,
            @Value("${AUTH_USER_HASH}") String userHash,@Value("${AUTH_ADMIN_HASH}") String adminHash) {
        return args->{
            add(accounts,users,"student","USER",userHash);
            add(accounts,users,"admin","ADMIN",adminHash);
        };
    }
    private void add(AuthAccounts accounts,UserRepository users,String name,String role,String hash) {
        if(accounts.existsById(name)) return; // Never reset an existing account at startup.
        if(!hash.matches("[0-9a-f]{96}")) throw new IllegalArgumentException("Expected salted PBKDF2 hash");
        String id=UUID.randomUUID().toString();
        users.save(new TradeUser(id,name));
        accounts.save(new AuthAccount(name,hash,role,id));
    }
}
