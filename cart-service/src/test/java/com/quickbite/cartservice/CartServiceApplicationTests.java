package com.quickbite.cartservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "eureka.client.enabled=false",
    "spring.boot.admin.client.enabled=false"
})
class CartServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
