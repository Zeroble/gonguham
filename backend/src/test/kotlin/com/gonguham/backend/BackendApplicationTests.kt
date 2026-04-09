package com.gonguham.backend

import com.gonguham.backend.support.PostgresIntegrationTest
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class BackendApplicationTests : PostgresIntegrationTest() {

	@Test
	fun contextLoads() {
	}

}
