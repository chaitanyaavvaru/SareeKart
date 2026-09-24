package com.sareekart.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.filter.CorsFilter;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Empirical Challenger Test Suite for Milestone 2:
 * 1. CORS Wildcard Pattern Matching (Vercel previews, Canonical domain, Hostile origin rejection)
 * 2. JVM Memory Flags & Port Binding (render.yaml, Dockerfile, application configs, 512MB RAM budget)
 */
class Milestone2EmpiricalChallengerTest {

    private static final String PROD_CORS_ALLOWED_ORIGINS = "https://sareekart.com,https://*.vercel.app";

    // =========================================================================
    // 1. CORS WILDCARD PATTERN MATCHING CHALLENGE
    // =========================================================================

    @ParameterizedTest(name = "CORS Positive Challenge: Valid origin {0} must be allowed with credentials")
    @ValueSource(strings = {
            "https://sareekart-preview-123.vercel.app",
            "https://sareekart-main.vercel.app",
            "https://sareekart-git-preview-feature-cart.vercel.app",
            "https://boutique-preview.vercel.app",
            "https://sareekart.vercel.app",
            "https://sareekart.com"
    })
    void challengeCorsAllowedOriginsWithCredentials(String validOrigin) throws Exception {
        CorsFilter filter = new CorsConfig(PROD_CORS_ALLOWED_ORIGINS).corsFilter();
        MockHttpServletRequest request = createPreflightRequest(validOrigin, "/api/products");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus())
                .as("Status code for preflight of valid origin " + validOrigin)
                .isEqualTo(200);
        assertThat(response.getHeader("Access-Control-Allow-Origin"))
                .as("Access-Control-Allow-Origin must echo back valid origin: " + validOrigin)
                .isEqualTo(validOrigin);
        assertThat(response.getHeader("Access-Control-Allow-Credentials"))
                .as("Access-Control-Allow-Credentials must be true")
                .isEqualTo("true");
        assertThat(response.getHeader("Access-Control-Max-Age"))
                .as("Access-Control-Max-Age should be 3600")
                .isEqualTo("3600");
    }

    @ParameterizedTest(name = "CORS Negative Challenge: Hostile or spoofed origin {0} must be rejected")
    @ValueSource(strings = {
            "https://evil-site.com",
            "http://malicious.com",
            "http://sareekart-preview-123.vercel.app",             // Insecure HTTP on vercel.app
            "http://sareekart.com",                               // Insecure HTTP on canonical domain
            "https://attacker-vercel.app.malicious.com",           // Domain suffix attack
            "https://evilvercel.app",                             // Missing subdomain dot squatting
            "https://vercel.app.evil.org",                        // Reverse prefix attack
            "https://notvercel.app",                              // Similar sounding TLD/domain
            "https://sareekart.com.evil.com",                     // Subdomain attack on canonical
            "https://sareekart-preview.vercel.app.attacker.com",  // Trailing attacker domain
            "https://evil-subdomain.vercel.app.attacker.net",     // Embedded vercel.app attack
            "https://evil-attacker.io",                           // Generic attacker
            "ftp://sareekart-preview-123.vercel.app"              // Insecure FTP scheme
    })
    void challengeCorsRejectsHostileOrigins(String hostileOrigin) throws Exception {
        CorsFilter filter = new CorsConfig(PROD_CORS_ALLOWED_ORIGINS).corsFilter();
        MockHttpServletRequest request = createPreflightRequest(hostileOrigin, "/api/products");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus())
                .as("Preflight response status for hostile origin " + hostileOrigin)
                .isEqualTo(403);
        assertThat(response.getHeader("Access-Control-Allow-Origin"))
                .as("Access-Control-Allow-Origin must be absent for hostile origin " + hostileOrigin)
                .isNull();
    }

    @Test
    @DisplayName("CORS Challenge: Preflight HTTP Methods and Headers verification")
    void challengeCorsAllowedMethodsAndHeaders() throws Exception {
        CorsFilter filter = new CorsConfig(PROD_CORS_ALLOWED_ORIGINS).corsFilter();
        MockHttpServletRequest request = createPreflightRequest("https://sareekart-preview-123.vercel.app", "/api/orders");
        request.addHeader("Access-Control-Request-Headers", "Authorization, Content-Type, X-Requested-With");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
        String allowMethods = response.getHeader("Access-Control-Allow-Methods");
        assertThat(allowMethods).isNotNull();
        assertThat(allowMethods).contains("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS");
        String allowHeaders = response.getHeader("Access-Control-Allow-Headers");
        assertThat(allowHeaders).isNotNull();
    }

    // =========================================================================
    // 2. JVM MEMORY OPTIMIZATION CHALLENGE (-Xmx384m -Xms128m -XX:+UseSerialGC -XX:TieredStopAtLevel=1)
    // =========================================================================

    @Test
    @DisplayName("JVM Memory Challenge: render.yaml specifies exact 512MB memory flags and blueprint spec")
    void challengeRenderYamlJvmMemoryFlags() throws Exception {
        Path renderYamlPath = Paths.get("../../render.yaml").toAbsolutePath().normalize();
        if (!Files.exists(renderYamlPath)) {
            // Fallback if cwd is root
            renderYamlPath = Paths.get("render.yaml").toAbsolutePath().normalize();
        }
        assertThat(renderYamlPath).exists();

        Yaml yaml = new Yaml();
        try (InputStream in = Files.newInputStream(renderYamlPath)) {
            Map<String, Object> doc = yaml.load(in);
            assertThat(doc).containsKey("services");

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> services = (List<Map<String, Object>>) doc.get("services");
            assertThat(services).isNotEmpty();

            Map<String, Object> webService = services.get(0);
            assertThat(webService.get("type")).isEqualTo("web");
            assertThat(webService.get("plan")).isEqualTo("free");
            assertThat(webService.get("healthCheckPath")).isEqualTo("/actuator/health");

            @SuppressWarnings("unchecked")
            List<Map<String, String>> envVars = (List<Map<String, String>>) webService.get("envVars");
            assertThat(envVars).isNotNull();

            String javaToolOptions = null;
            String corsAllowedOrigins = null;
            String hibernateDdlAuto = null;
            String neo4jEnabled = null;

            for (Map<String, String> env : envVars) {
                if ("JAVA_TOOL_OPTIONS".equals(env.get("key"))) {
                    javaToolOptions = env.get("value");
                }
                if ("CORS_ALLOWED_ORIGINS".equals(env.get("key"))) {
                    corsAllowedOrigins = env.get("value");
                }
                if ("SPRING_JPA_HIBERNATE_DDL_AUTO".equals(env.get("key"))) {
                    hibernateDdlAuto = env.get("value");
                }
                if ("NEO4J_ENABLED".equals(env.get("key"))) {
                    neo4jEnabled = env.get("value");
                }
            }

            assertThat(javaToolOptions)
                    .as("JAVA_TOOL_OPTIONS in render.yaml")
                    .isEqualTo("-Xmx384m -Xms128m -XX:+UseSerialGC -XX:TieredStopAtLevel=1");

            assertThat(corsAllowedOrigins)
                    .as("CORS_ALLOWED_ORIGINS in render.yaml")
                    .isEqualTo("https://sareekart.com,https://*.vercel.app");

            assertThat(hibernateDdlAuto)
                    .as("SPRING_JPA_HIBERNATE_DDL_AUTO in render.yaml")
                    .isEqualTo("validate");

            assertThat(neo4jEnabled)
                    .as("NEO4J_ENABLED in render.yaml")
                    .isEqualTo("false");
        }
    }

    @Test
    @DisplayName("JVM Memory Challenge: Dockerfile contains exact 512MB ENV JAVA_TOOL_OPTIONS and port 8081")
    void challengeDockerfileJvmMemoryFlags() throws Exception {
        Path dockerfilePath = Paths.get("Dockerfile").toAbsolutePath().normalize();
        assertThat(dockerfilePath).exists();

        String content = Files.readString(dockerfilePath);

        // Verify JAVA_TOOL_OPTIONS
        Pattern jvmPattern = Pattern.compile("ENV\\s+JAVA_TOOL_OPTIONS\\s*=\\s*[\"'](-Xmx384m\\s+-Xms128m\\s+-XX:\\+UseSerialGC\\s+-XX:TieredStopAtLevel=1)[\"']");
        Matcher jvmMatcher = jvmPattern.matcher(content);
        assertThat(jvmMatcher.find())
                .as("Dockerfile must define ENV JAVA_TOOL_OPTIONS with -Xmx384m -Xms128m -XX:+UseSerialGC -XX:TieredStopAtLevel=1")
                .isTrue();

        // Verify EXPOSE 8081
        assertThat(content)
                .as("Dockerfile must declare EXPOSE 8081")
                .contains("EXPOSE 8081");

        // Verify Java 17 runtime
        assertThat(content)
                .as("Dockerfile must use Eclipse Temurin 17 JRE")
                .contains("eclipse-temurin:17-jre-jammy");
    }

    // =========================================================================
    // 3. DYNAMIC PORT BINDING CHALLENGE (${PORT:8081})
    // =========================================================================

    @Test
    @DisplayName("Port Binding Challenge: Both application.yaml and application-prod.yaml configure dynamic ${PORT:8081}")
    void challengeDynamicPortConfiguration() throws Exception {
        Yaml yaml = new Yaml();

        // Check application.yaml
        try (InputStream in = getClass().getResourceAsStream("/application.yaml")) {
            assertThat(in).isNotNull();
            Map<String, Object> data = yaml.load(in);
            @SuppressWarnings("unchecked")
            Map<String, Object> server = (Map<String, Object>) data.get("server");
            assertThat(server).isNotNull();
            assertThat(String.valueOf(server.get("port")))
                    .as("application.yaml server.port")
                    .isEqualTo("${PORT:8081}");
        }

        // Check application-prod.yaml
        try (InputStream in = getClass().getResourceAsStream("/application-prod.yaml")) {
            assertThat(in).isNotNull();
            Map<String, Object> data = yaml.load(in);
            @SuppressWarnings("unchecked")
            Map<String, Object> server = (Map<String, Object>) data.get("server");
            assertThat(server).isNotNull();
            assertThat(String.valueOf(server.get("port")))
                    .as("application-prod.yaml server.port")
                    .isEqualTo("${PORT:8081}");
        }
    }

    @Test
    @DisplayName("Port Binding Challenge: Spring Environment resolves custom PORT and defaults to 8081")
    void challengeDynamicPortResolutionInSpringEnvironment() throws Exception {
        YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
        List<org.springframework.core.env.PropertySource<?>> propertySources =
                loader.load("application-prod.yaml", new ClassPathResource("application-prod.yaml"));
        assertThat(propertySources).isNotEmpty();

        // Scenario A: Custom PORT provided (e.g. Render assigns PORT=10000)
        StandardEnvironment customEnv = new StandardEnvironment();
        customEnv.getPropertySources().addFirst(new MapPropertySource("renderEnv", Map.of("PORT", "10000")));
        propertySources.forEach(ps -> customEnv.getPropertySources().addLast(ps));

        String resolvedCustomPort = customEnv.resolvePlaceholders("${server.port}");
        assertThat(resolvedCustomPort)
                .as("Resolved server.port when PORT=10000 is supplied")
                .isEqualTo("10000");

        // Scenario B: No PORT provided (defaults to 8081)
        StandardEnvironment defaultEnv = new StandardEnvironment();
        propertySources.forEach(ps -> defaultEnv.getPropertySources().addLast(ps));

        String resolvedDefaultPort = defaultEnv.resolvePlaceholders("${server.port}");
        assertThat(resolvedDefaultPort)
                .as("Resolved server.port when PORT is omitted")
                .isEqualTo("8081");
    }

    // =========================================================================
    // 4. RESOURCE AND CONNECTION POOL CONSTRAINTS FOR 512MB RAM
    // =========================================================================

    @Test
    @DisplayName("512MB RAM Budget Challenge: Verify Hikari pool, Tomcat threads, and Open-In-View in production config")
    void challenge512MbRamResourceConstraints() throws Exception {
        Yaml yaml = new Yaml();
        try (InputStream in = getClass().getResourceAsStream("/application-prod.yaml")) {
            assertThat(in).isNotNull();
            Map<String, Object> data = yaml.load(in);

            @SuppressWarnings("unchecked")
            Map<String, Object> server = (Map<String, Object>) data.get("server");
            @SuppressWarnings("unchecked")
            Map<String, Object> tomcat = (Map<String, Object>) server.get("tomcat");
            @SuppressWarnings("unchecked")
            Map<String, Object> threads = (Map<String, Object>) tomcat.get("threads");

            int maxThreads = Integer.parseInt(String.valueOf(threads.get("max")));
            int minSpare = Integer.parseInt(String.valueOf(threads.get("min-spare")));

            // Under 512MB, Tomcat threads must not exceed 25 to prevent stack overflow
            assertThat(maxThreads)
                    .as("Tomcat max threads must be <= 25")
                    .isLessThanOrEqualTo(25);
            assertThat(minSpare)
                    .as("Tomcat min-spare threads must be <= 5")
                    .isLessThanOrEqualTo(5);

            @SuppressWarnings("unchecked")
            Map<String, Object> spring = (Map<String, Object>) data.get("spring");
            @SuppressWarnings("unchecked")
            Map<String, Object> datasource = (Map<String, Object>) spring.get("datasource");
            @SuppressWarnings("unchecked")
            Map<String, Object> hikari = (Map<String, Object>) datasource.get("hikari");

            // Under 512MB and TiDB Cloud Serverless limits, pool size must be <= 8
            String maxPool = String.valueOf(hikari.get("maximum-pool-size"));
            assertThat(maxPool)
                    .as("Hikari maximum-pool-size default must be <= 8")
                    .contains(":8}");

            // Open-In-View must be disabled
            @SuppressWarnings("unchecked")
            Map<String, Object> jpa = (Map<String, Object>) spring.get("jpa");
            assertThat(jpa.get("open-in-view"))
                    .as("spring.jpa.open-in-view must be false in prod")
                    .isEqualTo(false);
        }
    }

    private MockHttpServletRequest createPreflightRequest(String origin, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", path);
        request.addHeader("Origin", origin);
        request.addHeader("Access-Control-Request-Method", "POST");
        request.addHeader("Access-Control-Request-Headers", "content-type, authorization");
        return request;
    }
}
