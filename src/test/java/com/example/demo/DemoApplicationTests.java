package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"spring.ai.openai.api-key=test-key",
		"management.tracing.export.otlp.enabled=false",
		"management.otlp.metrics.export.enabled=false"
})
class DemoApplicationTests {

	@Test
	void contextLoads() {
	}

}
