package com.quickbite.delivery;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
	"eureka.client.enabled=false",
	"spring.boot.admin.client.enabled=false"
})
class DeliveryServiceApplicationTests {

	@Test
	void contextLoads() {
	}
}
