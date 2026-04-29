package com.quickbite.review;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "spring.boot.admin.client.enabled=false"
})
class ReviewServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
