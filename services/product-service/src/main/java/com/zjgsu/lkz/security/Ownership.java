package com.zjgsu.lkz.security;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
public final class Ownership {
    private Ownership() {}
    public static void require(String userId) {
        var auth=SecurityContextHolder.getContext().getAuthentication();
        if(!(auth instanceof JwtAuthenticationToken jwt)) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        if(!jwt.getToken().getSubject().equals(userId) && auth.getAuthorities().stream().noneMatch(a->a.getAuthority().equals("ROLE_ADMIN")))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Resource belongs to another user");
    }
}
