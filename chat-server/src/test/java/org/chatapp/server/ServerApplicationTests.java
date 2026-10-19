package org.chatapp.server;

import org.chatapp.server.integration.config.MyTestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(MyTestConfiguration.class)
class ServerApplicationTests {

	@Test
	void contextLoads() {
	}

}
