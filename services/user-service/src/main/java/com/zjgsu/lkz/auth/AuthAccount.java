package com.zjgsu.lkz.auth;
import jakarta.persistence.*;
@Entity @Table(name="auth_accounts")
public class AuthAccount {
    @Id public String username;
    @Column(nullable=false,length=128) public String passwordHash;
    @Column(nullable=false) public String role;
    @Column(nullable=false) public String userId;
    protected AuthAccount() {}
    public AuthAccount(String username,String hash,String role,String userId) {
        this.username=username;this.passwordHash=hash;this.role=role;this.userId=userId;
    }
}
