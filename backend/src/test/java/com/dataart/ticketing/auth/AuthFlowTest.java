package com.dataart.ticketing.auth;

import java.util.Map;

import com.dataart.ticketing.domain.EmailVerificationToken;
import com.dataart.ticketing.mail.MailService;
import com.dataart.ticketing.repository.EmailVerificationTokenRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end backend business flow: sign up -> verify -> log in -> /me, plus the key
 * failure cases (unauthenticated, unverified, duplicate email). Runs against a real
 * PostgreSQL (Testcontainers). Mail sending is mocked so no real SMTP is contacted.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class AuthFlowTest {

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

    @MockBean
    MailService mailService; // avoid real SMTP during tests

    @Autowired
    TestRestTemplate rest;

    @Autowired
    EmailVerificationTokenRepository verificationTokens;

    @Test
    void signupVerifyLoginAndMe() {
        String email = "alice@example.com";
        String password = "password123";

        // Sign up -> 201
        ResponseEntity<Map> signup = rest.postForEntity(
                "/api/auth/signup", Map.of("email", email, "password", password), Map.class);
        assertThat(signup.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // Login before verifying -> 403 with the machine-readable code
        ResponseEntity<Map> unverified = rest.postForEntity(
                "/api/auth/login", Map.of("email", email, "password", password), Map.class);
        assertThat(unverified.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(unverified.getBody()).containsEntry("code", "email_not_verified");

        // Verify using the token issued during sign up -> 200 verified
        EmailVerificationToken token = verificationTokens.findAll().get(0);
        ResponseEntity<Map> verify = rest.getForEntity(
                "/api/auth/verify?token=" + token.getToken(), Map.class);
        assertThat(verify.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(verify.getBody()).containsEntry("status", "verified");

        // Login -> 200 with a bearer token
        ResponseEntity<Map> login = rest.postForEntity(
                "/api/auth/login", Map.of("email", email, "password", password), Map.class);
        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
        String jwt = (String) login.getBody().get("token");
        assertThat(jwt).isNotBlank();

        // /me without a token -> 401
        assertThat(rest.getForEntity("/api/auth/me", Map.class).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);

        // /me with the token -> 200 and the right identity
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwt);
        ResponseEntity<Map> me = rest.exchange(
                "/api/auth/me", HttpMethod.GET, new HttpEntity<>(headers), Map.class);
        assertThat(me.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(me.getBody()).containsEntry("email", email);
    }

    @Test
    void duplicateEmailReturns409() {
        Map<String, String> body = Map.of("email", "dup@example.com", "password", "password123");
        assertThat(rest.postForEntity("/api/auth/signup", body, Map.class).getStatusCode())
                .isEqualTo(HttpStatus.CREATED);
        assertThat(rest.postForEntity("/api/auth/signup", body, Map.class).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void invalidCredentialsReturn401() {
        ResponseEntity<Map> res = rest.postForEntity(
                "/api/auth/login", Map.of("email", "nobody@example.com", "password", "whatever12"),
                Map.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
