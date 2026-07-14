package com.dataart.ticketing;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Boots the full application against a real PostgreSQL (Testcontainers), which also
 * exercises the Flyway migration. Serves as the seed for backend business-flow tests.
 */
@SpringBootTest
@Testcontainers
class ApplicationContextTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("app.jwt.secret", () -> "test-secret-value-that-is-at-least-32-bytes-long");
    }

    @Autowired
    DataSource dataSource;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Test
    void contextLoadsAndMigrationsRun() {
        assertThat(dataSource).isNotNull();
    }

    @Test
    void passwordEncoderIsArgon2AndVerifies() {
        String hash = passwordEncoder.encode("s3cret-password");
        // Never stores plaintext; Argon2 encodings are prefixed with $argon2.
        assertThat(hash).startsWith("$argon2");
        assertThat(passwordEncoder.matches("s3cret-password", hash)).isTrue();
        assertThat(passwordEncoder.matches("wrong", hash)).isFalse();
    }
}
