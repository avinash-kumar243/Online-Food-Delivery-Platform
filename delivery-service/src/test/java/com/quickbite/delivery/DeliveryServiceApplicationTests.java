package com.quickbite.delivery;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
	"eureka.client.enabled=false",
	"spring.boot.admin.client.enabled=false",
	"spring.cloud.compatibility-verifier.enabled=false",
	"spring.datasource.url=jdbc:h2:mem:deliveryservice;DB_CLOSE_DELAY=-1;MODE=MySQL",
	"spring.datasource.driver-class-name=org.h2.Driver",
	"spring.datasource.username=sa",
	"spring.datasource.password=",
	"spring.jpa.hibernate.ddl-auto=create-drop"
})
class DeliveryServiceApplicationTests {

	@Test
	void contextLoads() {
	}
}
