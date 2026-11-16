package com.zjgsu.lkz.auth;
import java.time.Instant;
import java.util.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@ConditionalOnProperty(name="app.security.enabled",havingValue="true",matchIfMissing=true)
public class AuthController {
    private final AuthAccounts accounts;
    private final PasswordEncoder passwords;
    private final JwtEncoder encoder;
    private final String dummyHash;
    public AuthController(AuthAccounts accounts,PasswordEncoder passwords,JwtEncoder encoder) {
        this.accounts=accounts;this.passwords=passwords;this.encoder=encoder;
        dummyHash=passwords.encode(UUID.randomUUID().toString());
    }
    public record Login(@NotBlank @Size(max=80) String username,@NotBlank @Size(max=200) String password) {}
    @PostMapping("/api/auth/login")
    public Map<String,Object> login(@Valid @RequestBody Login request) {
        var account=accounts.findById(request.username()).orElse(null);
        boolean matched=passwords.matches(request.password(),account==null?dummyHash:account.passwordHash);
        if(account==null || !matched) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Invalid credentials");
        Instant now=Instant.now();
        var claims=JwtClaimsSet.builder().issuer("campustrade-user-service").subject(account.userId)
            .claim("username",account.username).claim("roles",List.of(account.role))
            .issuedAt(now).expiresAt(now.plusSeconds(1800)).build();
        String token=encoder.encode(JwtEncoderParameters.from(
            JwsHeader.with(SignatureAlgorithm.RS256).build(),claims)).getTokenValue();
        return Map.of("access_token",token,"token_type","Bearer","expires_in",1800,"roles",List.of(account.role));
    }
    @GetMapping("/api/auth/me")
    public Map<String,Object> me(@AuthenticationPrincipal Jwt jwt,HttpServletRequest request) {
        return Map.of("userId",jwt.getSubject(),"username",jwt.getClaimAsString("username"),
            "roles",jwt.getClaimAsStringList("roles"),
            "forwardedUserId",Objects.toString(request.getHeader("X-User-Id"),""),
            "forwardedRoles",Objects.toString(request.getHeader("X-User-Roles"),""));
    }
    @GetMapping("/api/admin/users")
    public List<Map<String,String>> users() {
        // Explicit response DTO: never serialize the account entity/password hash.
        return accounts.findAll().stream().map(a->Map.of("username",a.username,"role",a.role,"userId",a.userId)).toList();
    }
}
