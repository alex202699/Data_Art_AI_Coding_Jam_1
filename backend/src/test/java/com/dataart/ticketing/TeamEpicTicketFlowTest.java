package com.dataart.ticketing;

import java.util.Map;
import java.util.UUID;

import com.dataart.ticketing.auth.JwtService;
import com.dataart.ticketing.domain.User;
import com.dataart.ticketing.mail.MailService;
import com.dataart.ticketing.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end backend business flow for the board features: a verified user creates a team,
 * an epic, and a ticket, moves it across states, comments on it, and deletes it — asserting
 * the key requirements (counts, referenced-delete 409s, epic-same-team, enum validation,
 * and the modified_at semantics: state change advances it, adding a comment does not).
 * Runs against a real PostgreSQL via Testcontainers; mail is mocked.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@SuppressWarnings({"rawtypes", "unchecked"})
class TeamEpicTicketFlowTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("app.jwt.secret", () -> "test-secret-value-that-is-at-least-32-bytes-long");
    }

    @MockBean
    MailService mailService;

    @Autowired TestRestTemplate rest;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtService jwtService;

    private String token;

    @BeforeEach
    void createVerifiedUserAndToken() {
        User user = new User("qa+" + UUID.randomUUID() + "@example.com",
                passwordEncoder.encode("password123"));
        user.setEmailVerified(true);
        user = users.save(user);
        token = jwtService.issue(user.getId());
        // JDK HttpClient factory supports PATCH (the default HttpURLConnection does not).
        rest.getRestTemplate().setRequestFactory(new JdkClientHttpRequestFactory());
    }

    @Test
    void teamEpicTicketCommentHappyPath() {
        String teamId = (String) create("/api/teams", Map.of("name", "Payments " + UUID.randomUUID()),
                "team").get("id");
        String epicId = (String) create("/api/epics",
                Map.of("teamId", teamId, "title", "Checkout", "description", "Recovery"),
                "epic").get("id");

        // Create a ticket referencing the epic.
        Map ticket = create("/api/tickets", Map.of(
                "teamId", teamId, "epicId", epicId, "type", "bug",
                "title", "Payment fails", "body", "Repro steps"), "ticket");
        String ticketId = (String) ticket.get("id");
        assertThat(ticket.get("state")).isEqualTo("new");
        assertThat(ticket.get("epicTitle")).isEqualTo("Checkout");
        assertThat((String) ticket.get("createdByEmail")).contains("@example.com");

        // Listing returns it for the team.
        ResponseEntity<Map> list = get("/api/tickets?teamId=" + teamId);
        assertThat(((java.util.List<?>) list.getBody().get("tickets"))).hasSize(1);

        // Drag to a new state -> 200 and modified_at advances. Read persisted values via GET
        // (Postgres stores microsecond precision) so string comparisons are stable.
        String before = (String) ticket.get("modifiedAt");
        Map moved = patch("/api/tickets/" + ticketId + "/state", Map.of("state", "in_progress"), "ticket");
        assertThat(moved.get("state")).isEqualTo("in_progress");
        String afterMove = (String) ((Map) get("/api/tickets/" + ticketId).getBody().get("ticket"))
                .get("modifiedAt");
        assertThat(afterMove).isNotEqualTo(before);

        // Add two comments -> 201; ticket modified_at must NOT change.
        assertThat(exchange("/api/tickets/" + ticketId + "/comments", HttpMethod.POST,
                Map.of("body", "Reproduced in Chrome."), Map.class).getStatusCode())
                .isEqualTo(HttpStatus.CREATED);
        exchange("/api/tickets/" + ticketId + "/comments", HttpMethod.POST,
                Map.of("body", "Backend returns 500."), Map.class);
        Map afterCommentTicket = (Map) get("/api/tickets/" + ticketId).getBody().get("ticket");
        assertThat(afterCommentTicket.get("modifiedAt")).isEqualTo(afterMove);

        // Comments listed oldest-first.
        ResponseEntity<Map> comments = get("/api/tickets/" + ticketId + "/comments");
        java.util.List<Map> items = (java.util.List<Map>) comments.getBody().get("comments");
        assertThat(items).hasSize(2);
        assertThat(items.get(0).get("body")).isEqualTo("Reproduced in Chrome.");

        // Delete ticket -> 204, then it is gone (comments cascaded).
        assertThat(exchange("/api/tickets/" + ticketId, HttpMethod.DELETE, null, Void.class)
                .getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(get("/api/tickets/" + ticketId).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void enforcesBusinessRules() {
        String teamA = (String) create("/api/teams", Map.of("name", "A " + UUID.randomUUID()), "team").get("id");
        String teamB = (String) create("/api/teams", Map.of("name", "B " + UUID.randomUUID()), "team").get("id");
        String epicA = (String) create("/api/epics", Map.of("teamId", teamA, "title", "EpicA"), "epic").get("id");

        // Epic from a different team -> 400.
        assertThat(exchange("/api/tickets", HttpMethod.POST, Map.of(
                "teamId", teamB, "epicId", epicA, "type", "bug", "title", "T", "body", "B"),
                Map.class).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        // Invalid enum -> 400.
        assertThat(exchange("/api/tickets", HttpMethod.POST, Map.of(
                "teamId", teamA, "type", "bogus", "title", "T", "body", "B"),
                Map.class).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        // Create a ticket under epicA, then referenced-delete guards fire (409).
        String ticketId = (String) create("/api/tickets", Map.of(
                "teamId", teamA, "epicId", epicA, "type", "feature", "title", "T", "body", "B"),
                "ticket").get("id");
        assertThat(exchange("/api/epics/" + epicA, HttpMethod.DELETE, null, Map.class)
                .getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(exchange("/api/teams/" + teamA, HttpMethod.DELETE, null, Map.class)
                .getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        // Empty comment -> 400.
        assertThat(exchange("/api/tickets/" + ticketId + "/comments", HttpMethod.POST,
                Map.of("body", "   "), Map.class).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        // Unauthenticated -> 401.
        assertThat(rest.getForEntity("/api/tickets?teamId=" + teamA, Map.class).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // --- helpers ---

    private HttpHeaders headers() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setBearerAuth(token);
        return h;
    }

    private Map create(String url, Object body, String key) {
        ResponseEntity<Map> res = exchange(url, HttpMethod.POST, body, Map.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return (Map) res.getBody().get(key);
    }

    private Map patch(String url, Object body, String key) {
        ResponseEntity<Map> res = exchange(url, HttpMethod.PATCH, body, Map.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        return (Map) res.getBody().get(key);
    }

    private ResponseEntity<Map> get(String url) {
        return exchange(url, HttpMethod.GET, null, Map.class);
    }

    private <T> ResponseEntity<T> exchange(String url, HttpMethod method, Object body, Class<T> type) {
        return rest.exchange(url, method, new HttpEntity<>(body, headers()), type);
    }
}
