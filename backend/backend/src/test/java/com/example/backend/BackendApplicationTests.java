package com.example.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import com.sareekart.SareeKartApplication;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = SareeKartApplication.class)
@ActiveProfiles("test")
class BackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
