package com.sareekart;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "app.storage.provider=local")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ImageUploadIntegrationTest {

    @Autowired private TestRestTemplate rest;
    private static String customerToken, adminToken;
    private static long productId;

    @SuppressWarnings("unchecked")
    private String login(String email, String password) {
        var res = rest.exchange("/auth/login", HttpMethod.POST,
            new HttpEntity<>(Map.of("email", email, "password", password), json(null)), Map.class);
        assertThat(res.getStatusCode().value()).isEqualTo(200);
        return ((Map<?, ?>) res.getBody().get("data")).get("token").toString();
    }

    private HttpHeaders json(String token) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        if (token != null && !token.isBlank()) h.setBearerAuth(token);
        return h;
    }

    private ResponseEntity<Map> upload(long pid, byte[] content,
                                        String contentType, String originalFilename,
                                        String token) {
        var filePart = new org.springframework.mock.web.MockMultipartFile(
            "files", originalFilename, contentType, content);

        var parts = new org.springframework.util.LinkedMultiValueMap<String, Object>();
        parts.add("files", filePart.getResource());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        if (token != null && !token.isBlank()) headers.setBearerAuth(token);

        var requestEntity = new HttpEntity<>(parts, headers);
        return rest.exchange("/products/" + pid + "/images", HttpMethod.POST,
            requestEntity, Map.class);
    }

    private long createProduct(String skuSuffix) {
        HttpHeaders adminAuth = json(adminToken);
        var res = rest.exchange("/admin/products", HttpMethod.POST,
            new HttpEntity<>(Map.of("name", "Image Test Saree", "categoryId", 1,
                "basePrice", 1000, "sku", "SK-IMG-" + skuSuffix), adminAuth), Map.class);
        assertThat(res.getStatusCode().value()).isEqualTo(200);
        return ((Number) ((Map<?, ?>) res.getBody().get("data")).get("id")).longValue();
    }

    // ── flow ─────────────────────────────────────────────────────────────────

    @Test
    @Order(1)
    void setup() {
        customerToken = login("customer@sareekart.com", "customer123");
        adminToken = login("admin@sareekart.com", "admin123");
        productId = createProduct(String.valueOf(System.nanoTime() % 1_000_000_000L));
    }

    @Test
    @Order(2)
    @DisplayName("valid JPEG upload succeeds")
    void validUpload() {
        byte[] jpeg = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};
        var res = upload(productId, jpeg, "image/jpeg", "saree.jpg", adminToken);
        assertThat(res.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    @Order(3)
    @DisplayName("unsupported MIME rejected")
    void unsupportedMimeRejected() {
        var res = upload(productId, "malicious".getBytes(), "application/pdf", "evil.pdf", adminToken);
        assertThat(res.getStatusCode().value()).isEqualTo(400);
    }

    @Test
    @Order(4)
    @DisplayName("executable magic-bytes rejected even with image Content-Type")
    void executableRejected() {
        byte[] elf = {0x7F, 'E', 'L', 'F', 0x02};
        var res = upload(productId, elf, "image/jpeg", "trojan.jpg", adminToken);
        assertThat(res.getStatusCode().value()).isEqualTo(400);
    }

    @Test
    @Order(5)
    @DisplayName("empty file rejected")
    void emptyFileRejected() {
        var res = upload(productId, new byte[0], "image/png", "empty.png", adminToken);
        assertThat(res.getStatusCode().value()).isEqualTo(400);
    }

    @Test
    @Order(6)
    @DisplayName("customer and anonymous uploads rejected")
    void authorizationBoundaries() {
        byte[] jpeg = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};

        ResponseEntity<Map> anon = upload(productId, jpeg, "image/jpeg", "a.jpg", null);
        assertThat(anon.getStatusCode().value()).isIn(401, 403);

        ResponseEntity<Map> cust = upload(productId, jpeg, "image/jpeg", "b.jpg", customerToken);
        assertThat(cust.getStatusCode().value()).isEqualTo(403);
    }

    @Test
    @Order(7)
    @DisplayName("delete idempotent — nonexistent URL doesn't throw")
    void deleteIdempotent() {
        ResponseEntity<Map> res = rest.exchange(
            "/products/" + productId + "/images?url=/uploads/products/nonexistent.png",
            HttpMethod.DELETE, new HttpEntity<>(json(adminToken)), Map.class);
        assertThat(res.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    @Order(8)
    @DisplayName("product retrieval regression after image ops")
    void productRetrievalRegression() {
        ResponseEntity<Map> res = rest.getForEntity("/products/" + productId, Map.class);
        assertThat(res.getStatusCode().value()).isEqualTo(200);
        Map<?, ?> product = ((Map<?, ?>) res.getBody().get("data"));
        assertThat(product.get("name")).isEqualTo("Image Test Saree");
    }
}