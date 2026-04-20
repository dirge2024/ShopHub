package com.shophub.consultant;

import com.shophub.consultant.mapper.ShopMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class ConsultantApplicationTests {

    @Autowired
    private ShopMapper shopMapper;

    @Test
    void contextLoads() {
        assertNotNull(shopMapper);
    }
}
