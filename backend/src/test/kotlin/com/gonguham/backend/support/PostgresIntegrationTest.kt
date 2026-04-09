package com.gonguham.backend.support

import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.postgresql.PostgreSQLContainer

abstract class PostgresIntegrationTest {
    companion object {
        private val postgres =
            PostgreSQLContainer("postgres:17-alpine")
                .withDatabaseName("gonguham_test")
                .withUsername("gonguham")
                .withPassword("gonguham")

        @JvmStatic
        @DynamicPropertySource
        fun registerPostgresProperties(registry: DynamicPropertyRegistry) {
            if (!postgres.isRunning) {
                postgres.start()
            }

            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
            registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName)
        }
    }
}
