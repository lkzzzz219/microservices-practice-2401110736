package com.zjgsu.lkz.model;
import jakarta.persistence.*;
@Entity @Table(name="users")
public class TradeUser {
    @Id public String id;
    @Column(nullable=false, length=80) public String displayName;
    protected TradeUser() {}
    public TradeUser(String id, String name) { this.id=id; this.displayName=name; }
}
