package com.hendro.ecommerce;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestOAuth2ClientConfig.class)
class SpringBootEcommerceApplicationTests {

	@Test
	void contextLoads() {
	}

}
