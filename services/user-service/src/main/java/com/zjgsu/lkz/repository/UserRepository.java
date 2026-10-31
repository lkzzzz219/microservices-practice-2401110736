package com.zjgsu.lkz.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.zjgsu.lkz.model.TradeUser;
public interface UserRepository extends JpaRepository<TradeUser,String> {}
