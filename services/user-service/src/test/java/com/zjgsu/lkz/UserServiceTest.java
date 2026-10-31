package com.zjgsu.lkz;
import com.zjgsu.lkz.service.UserService;
import com.zjgsu.lkz.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
class UserServiceTest {
    @Autowired UserService users;
    @Test void createAndRead() { var u=users.create("林昆泽"); assertEquals("林昆泽",users.get(u.id).displayName); }
    @Test void missingUser() { assertThrows(ResourceNotFoundException.class,()->users.get("missing")); }
}
