package com.zjgsu.lkz.controller;
import com.zjgsu.lkz.service.UserService;
import com.zjgsu.lkz.model.TradeUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
@RestController @RequestMapping("/api/users")
public class UserController {
    private final UserService users;
    public UserController(UserService users) { this.users=users; }
    public record CreateUser(@NotBlank @Size(max=80) String displayName) {}
    @PostMapping public ResponseEntity<TradeUser> create(@Valid @RequestBody CreateUser request) {
        var user=users.create(request.displayName());
        return ResponseEntity.created(URI.create("/api/users/"+user.id)).body(user);
    }
    @GetMapping("/{id}") public TradeUser get(@PathVariable String id) { return users.get(id); }
}
