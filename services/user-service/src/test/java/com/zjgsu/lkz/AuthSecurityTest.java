package com.zjgsu.lkz;

import java.security.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.security.crypto.password.*;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.*;
import org.springframework.web.client.*;
import com.zjgsu.lkz.auth.AuthAccounts;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties={"app.security.enabled=true","spring.datasource.url=jdbc:h2:mem:auth-security;DB_CLOSE_DELAY=-1"})
class AuthSecurityTest {
    static final String PASSWORD=UUID.randomUUID().toString();
    static final KeyPair KEYS;
    static final PasswordEncoder ENCODER=new Pbkdf2PasswordEncoder("",16,600000,
        Pbkdf2PasswordEncoder.SecretKeyFactoryAlgorithm.PBKDF2WithHmacSHA256);
    static {
        try { var gen=KeyPairGenerator.getInstance("RSA");gen.initialize(2048);KEYS=gen.generateKeyPair(); }
        catch(Exception e){throw new RuntimeException(e);}
    }
    @DynamicPropertySource static void properties(DynamicPropertyRegistry r) {
        r.add("JWT_PUBLIC_KEY",()->Base64.getEncoder().encodeToString(KEYS.getPublic().getEncoded()));
        r.add("JWT_PRIVATE_KEY",()->Base64.getEncoder().encodeToString(KEYS.getPrivate().getEncoded()));
        r.add("AUTH_USER_HASH",()->ENCODER.encode(PASSWORD));
        r.add("AUTH_ADMIN_HASH",()->ENCODER.encode(PASSWORD));
    }
    @Autowired Environment env;
    @Autowired AuthAccounts accounts;
    @Autowired JwtDecoder decoder;
    RestClient client(){return RestClient.create("http://localhost:"+env.getProperty("local.server.port"));}
    String token(String username) {
        var body=client().post().uri("/api/auth/login").body(Map.of("username",username,"password",PASSWORD))
            .retrieve().body(Map.class);
        return (String)body.get("access_token");
    }
    @Test void hashesAreSaltedAndMatchOnlyCorrectPassword() {
        var user=accounts.findById("student").orElseThrow();
        var admin=accounts.findById("admin").orElseThrow();
        assertNotEquals(PASSWORD,user.passwordHash);
        assertNotEquals(user.passwordHash,admin.passwordHash);
        assertTrue(ENCODER.matches(PASSWORD,user.passwordHash));
        assertFalse(ENCODER.matches("wrong",user.passwordHash));
    }
    @Test void loginIssuesSignedExpiringRoleToken() {
        var jwt=decoder.decode(token("student"));
        assertEquals("campustrade-user-service",jwt.getClaimAsString("iss"));
        assertEquals(List.of("USER"),jwt.getClaimAsStringList("roles"));
        assertEquals(1800,jwt.getExpiresAt().getEpochSecond()-jwt.getIssuedAt().getEpochSecond());
    }
    @Test void missingTokenAndWrongPasswordReturn401() {
        assertEquals(401,assertThrows(HttpClientErrorException.class,()->client().get().uri("/api/auth/me").retrieve().toBodilessEntity()).getStatusCode().value());
        assertEquals(401,assertThrows(HttpClientErrorException.class,()->client().post().uri("/api/auth/login")
            .body(Map.of("username","student","password","wrong")).retrieve().toBodilessEntity()).getStatusCode().value());
    }
    @Test void roleEnforcedEvenWhenHeadersClaimAdmin() {
        assertEquals(403,assertThrows(HttpClientErrorException.class,()->client().get().uri("/api/admin/users")
            .headers(h->{h.setBearerAuth(token("student"));h.set("X-User-Roles","ADMIN");})
            .retrieve().toBodilessEntity()).getStatusCode().value());
        var result=client().get().uri("/api/admin/users").headers(h->h.setBearerAuth(token("admin")))
            .retrieve().body(String.class);
        assertFalse(result.contains("password"));
    }
}
