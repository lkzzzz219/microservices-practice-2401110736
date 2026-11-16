package com.zjgsu.lkz.auth;
import org.springframework.data.jpa.repository.JpaRepository;
public interface AuthAccounts extends JpaRepository<AuthAccount,String> {}
