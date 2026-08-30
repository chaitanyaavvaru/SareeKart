package com.sareekart;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Full-context integration test against a real MySQL database.
 *
 * Requires a reachable database via standard Spring datasource environment
 * variables (SPRING_DATASOURCE_URL / USERNAME / PASSWORD). CI provisions a
 * MySQL service container; locally export them before running `mvnw test`.
 *
 * Deliberately asserts the routing rules that previously regressed:
 * public catalog access, protected cart access, and seeded admin login.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SareeKartApplicationTests {

    @Autowired
    private TestRestTemplate rest;

    @Test
    @DisplayName("context loads and health endpoint is public")
    void contextLoadsAndHealthIsPublic() {
        ResponseEntity<Map> response = rest.getForEntity("/health", Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("status", "UP");
    }

    @Test
    @DisplayName("product catalog is publicly readable without a token")
    void catalogIsPublic() {
        // NOTE: context-path=/api means security matchers must NOT carry the
        // /api prefix. This assertion guards that invariant.
        ResponseEntity<String> products =
            rest.getForEntity("/products?page=0&size=1", String.class);
        assertThat(products.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("cart requires authentication")
    void cartIsProtected() {
        ResponseEntity<String> response = rest.getForEntity("/cart", String.class);
        assertThat(response.getStatusCode()).isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("admin endpoints reject anonymous callers")
    void adminRequiresAuth() {
        ResponseEntity<String> response = rest.getForEntity("/admin/dashboard", String.class);
        assertThat(response.getStatusCode()).isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("seeded admin can authenticate and read the dashboard")
    void seededAdminLoginAndDashboard() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<Map> login = rest.exchange(
            "/auth/login",
            HttpMethod.POST,
            new HttpEntity<>(Map.of(
                "email", "admin@sareekart.com",
                "password", "admin123"), headers),
            Map.class);

        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
        String token = ((Map<?, ?>) login.getBody().get("data")).get("token").toString();
        assertThat(token).isNotBlank();

        HttpHeaders authed = new HttpHeaders();
        authed.setBearerAuth(token);
        ResponseEntity<Map> dashboard = rest.exchange(
            "/admin/dashboard", HttpMethod.GET, new HttpEntity<>(authed), Map.class);

        assertThat(dashboard.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(dashboard.getBody().get("success")).isEqualTo(true);
    }
}