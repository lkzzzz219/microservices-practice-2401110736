package com.zjgsu.lkz.service;
import org.springframework.stereotype.Service;
import com.zjgsu.lkz.repository.UserRepository;
import com.zjgsu.lkz.model.TradeUser;
import com.zjgsu.lkz.exception.ResourceNotFoundException;
import java.util.UUID;
@Service
public class UserService {
    private final UserRepository users;
    public UserService(UserRepository users) { this.users=users; }
    public TradeUser create(String name) { return users.save(new TradeUser(UUID.randomUUID().toString(),name)); }
    public TradeUser get(String id) { return users.findById(id).orElseThrow(() -> new ResourceNotFoundException("用户不存在: "+id)); }
}
