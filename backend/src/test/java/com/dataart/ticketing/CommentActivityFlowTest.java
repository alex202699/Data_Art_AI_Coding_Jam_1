package com.dataart.ticketing;

import java.util.List;
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
 * Stretch features: comment edit/delete (author-only) and ticket activity history.
 * Verifies the 403 ownership guard, edited_at stamping, that editing a comment does not
 * bump the ticket's modified_at, and that the activity feed records created/state_changed/
 * comment_added oldest-first.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@SuppressWarnings({"rawtypes", "unchecked"})
class CommentActivityFlowTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("app.jwt.secret", () -> "test-secret-value-that-is-at-least-32-bytes-long");
    }

    @MockBean MailService mailService;
    @Autowired TestRestTemplate rest;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtService jwtService;

    private String tokenA;
    private String tokenB;

    @BeforeEach
    void setup() {
        rest.getRestTemplate().setRequestFactory(new JdkClientHttpRequestFactory());
        tokenA = tokenFor("a+" + UUID.randomUUID() + "@example.com");
        tokenB = tokenFor("b+" + UUID.randomUUID() + "@example.com");
    }

    private String tokenFor(String email) {
        User u = new User(email, passwordEncoder.encode("password123"));
        u.setEmailVerified(true);
        return jwtService.issue(users.save(u).getId());
    }

    @Test
    void commentEditDeleteOwnershipAndEditedAt() {
        String ticketId = seedTicket();
        String commentId = (String) create(tokenA, "/api/tickets/" + ticketId + "/comments",
                Map.of("body", "First note"), "comment").get("id");

        // Another user cannot edit or delete it.
        assertThat(exchange(tokenB, "/api/tickets/" + ticketId + "/comments/" + commentId,
                HttpMethod.PATCH, Map.of("body", "hijack"), Map.class).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(exchange(tokenB, "/api/tickets/" + ticketId + "/comments/" + commentId,
                HttpMethod.DELETE, null, Map.class).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);

        // The author edits it -> 200 and edited_at is set.
        Map edited = (Map) exchange(tokenA, "/api/tickets/" + ticketId + "/comments/" + commentId,
                HttpMethod.PATCH, Map.of("body", "Edited note"), Map.class).getBody().get("comment");
        assertThat(edited.get("body")).isEqualTo("Edited note");
        assertThat(edited.get("editedAt")).isNotNull();

        // The author deletes it -> 204.
        assertThat(exchange(tokenA, "/api/tickets/" + ticketId + "/comments/" + commentId,
                HttpMethod.DELETE, null, Void.class).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void editingCommentDoesNotBumpTicketModifiedAt() {
        String ticketId = seedTicket();
        String before = (String) ((Map) get(tokenA, "/api/tickets/" + ticketId).getBody().get("ticket"))
                .get("modifiedAt");
        String commentId = (String) create(tokenA, "/api/tickets/" + ticketId + "/comments",
                Map.of("body", "note"), "comment").get("id");
        exchange(tokenA, "/api/tickets/" + ticketId + "/comments/" + commentId, HttpMethod.PATCH,
                Map.of("body", "note edited"), Map.class);
        String after = (String) ((Map) get(tokenA, "/api/tickets/" + ticketId).getBody().get("ticket"))
                .get("modifiedAt");
        assertThat(after).isEqualTo(before);
    }

    @Test
    void activityFeedRecordsEventsOldestFirst() {
        String ticketId = seedTicket();
        patch(tokenA, "/api/tickets/" + ticketId + "/state", Map.of("state", "in_progress"));
        create(tokenA, "/api/tickets/" + ticketId + "/comments", Map.of("body", "a comment"), "comment");

        List<Map> feed = (List<Map>) get(tokenA, "/api/tickets/" + ticketId + "/activity")
                .getBody().get("activity");
        List<String> kinds = feed.stream().map(m -> (String) m.get("kind")).toList();
        assertThat(kinds).containsExactly("created", "state_changed", "comment_added");
        assertThat(feed.get(1).get("field")).isEqualTo("state");
        assertThat(feed.get(1).get("newValue")).isEqualTo("in_progress");
    }

    // --- helpers ---

    private String seedTicket() {
        String teamId = (String) create(tokenA, "/api/teams",
                Map.of("name", "T " + UUID.randomUUID()), "team").get("id");
        return (String) create(tokenA, "/api/tickets",
                Map.of("teamId", teamId, "type", "bug", "title", "T", "body", "B"), "ticket").get("id");
    }

    private HttpHeaders headers(String token) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setBearerAuth(token);
        return h;
    }

    private Map create(String token, String url, Object body, String key) {
        ResponseEntity<Map> res = exchange(token, url, HttpMethod.POST, body, Map.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return (Map) res.getBody().get(key);
    }

    private Map patch(String token, String url, Object body) {
        return (Map) exchange(token, url, HttpMethod.PATCH, body, Map.class).getBody().get("ticket");
    }

    private ResponseEntity<Map> get(String token, String url) {
        return exchange(token, url, HttpMethod.GET, null, Map.class);
    }

    private <T> ResponseEntity<T> exchange(String token, String url, HttpMethod method, Object body, Class<T> type) {
        return rest.exchange(url, method, new HttpEntity<>(body, headers(token)), type);
    }
}
