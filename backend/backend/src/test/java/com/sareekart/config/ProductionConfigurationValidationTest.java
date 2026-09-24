package com.sareekart.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ProductionConfigurationValidationTest {

    @Test
    @DisplayName("Verify production application-prod.yaml strictly enforces ddl-auto=validate without silent fallback to update")
    void testProductionDdlAutoPolicy() {
        Yaml yaml = new Yaml();
        try (InputStream in = getClass().getResourceAsStream("/application-prod.yaml")) {
            assertThat(in).isNotNull();
            Map<String, Object> data = yaml.load(in);

            assertThat(data).containsKey("spring");
            @SuppressWarnings("unchecked")
            Map<String, Object> spring = (Map<String, Object>) data.get("spring");

            assertThat(spring).containsKey("jpa");
            @SuppressWarnings("unchecked")
            Map<String, Object> jpa = (Map<String, Object>) spring.get("jpa");

            assertThat(jpa).containsKey("hibernate");
            @SuppressWarnings("unchecked")
            Map<String, Object> hibernate = (Map<String, Object>) jpa.get("hibernate");

            String ddlAuto = String.valueOf(hibernate.get("ddl-auto"));

            // Must either be literally "validate" or have default fallback ":validate}"
            assertThat(ddlAuto)
                    .matches("validate|\\$\\{SPRING_JPA_HIBERNATE_DDL_AUTO:validate\\}");

            // Explicitly assert it NEVER falls back to unsafe DDL modes
            assertThat(ddlAuto).doesNotContain(":update");
            assertThat(ddlAuto).doesNotContain(":create");
            assertThat(ddlAuto).doesNotContain(":create-drop");
        } catch (Exception e) {
            throw new RuntimeException("Failed to read application-prod.yaml", e);
        }
    }

    @Test
    @DisplayName("Verify production application-prod.yaml configures Flyway for clean-room migration with baseline-on-migrate=false")
    void testProductionFlywayConfiguration() {
        Yaml yaml = new Yaml();
        try (InputStream in = getClass().getResourceAsStream("/application-prod.yaml")) {
            assertThat(in).isNotNull();
            Map<String, Object> data = yaml.load(in);

            @SuppressWarnings("unchecked")
            Map<String, Object> spring = (Map<String, Object>) data.get("spring");
            assertThat(spring).containsKey("flyway");

            @SuppressWarnings("unchecked")
            Map<String, Object> flyway = (Map<String, Object>) spring.get("flyway");

            // Flyway must be enabled by default
            assertThat(String.valueOf(flyway.get("enabled"))).matches("true|\\$\\{SPRING_FLYWAY_ENABLED:true\\}");

            // baseline-on-migrate must default to false for clean-room migration (empty DB -> V1..V32)
            String baselineOnMigrate = String.valueOf(flyway.get("baseline-on-migrate"));
            assertThat(baselineOnMigrate).matches("false|\\$\\{SPRING_FLYWAY_BASELINE_ON_MIGRATE:false\\}");

            // Clean must be disabled in production to protect data
            assertThat(flyway.get("clean-disabled")).isEqualTo(true);

            // Validate on migrate must be enabled
            assertThat(flyway.get("validate-on-migrate")).isEqualTo(true);

            // Migration scripts must reside in classpath:db/migration
            assertThat(flyway.get("locations")).isEqualTo("classpath:db/migration");
        } catch (Exception e) {
            throw new RuntimeException("Failed to read application-prod.yaml", e);
        }
    }

    @Test
    @DisplayName("Verify production application-prod.yaml reads DataSource credentials from environment variables")
    void testProductionDataSourceConfiguration() {
        Yaml yaml = new Yaml();
        try (InputStream in = getClass().getResourceAsStream("/application-prod.yaml")) {
            assertThat(in).isNotNull();
            Map<String, Object> data = yaml.load(in);

            @SuppressWarnings("unchecked")
            Map<String, Object> spring = (Map<String, Object>) data.get("spring");
            assertThat(spring).containsKey("datasource");

            @SuppressWarnings("unchecked")
            Map<String, Object> datasource = (Map<String, Object>) spring.get("datasource");

            assertThat(datasource.get("url")).isEqualTo("${SPRING_DATASOURCE_URL}");
            assertThat(datasource.get("username")).isEqualTo("${SPRING_DATASOURCE_USERNAME}");
            assertThat(datasource.get("password")).isEqualTo("${SPRING_DATASOURCE_PASSWORD}");
        } catch (Exception e) {
            throw new RuntimeException("Failed to read application-prod.yaml", e);
        }
    }

    @Test
    @DisplayName("Verify production application-prod.yaml enforces 512MB RAM optimizations: capped Hikari pool, capped Tomcat threads, open-in-view disabled, and dynamic port")
    void testProductionMemoryAndPortOptimizations() {
        Yaml yaml = new Yaml();
        try (InputStream in = getClass().getResourceAsStream("/application-prod.yaml")) {
            assertThat(in).isNotNull();
            Map<String, Object> data = yaml.load(in);

            // Verify server port is dynamic ${PORT:8081}
            @SuppressWarnings("unchecked")
            Map<String, Object> server = (Map<String, Object>) data.get("server");
            assertThat(server).isNotNull();
            assertThat(String.valueOf(server.get("port"))).isEqualTo("${PORT:8081}");

            // Verify Tomcat threads capped for 512MB RAM
            @SuppressWarnings("unchecked")
            Map<String, Object> tomcat = (Map<String, Object>) server.get("tomcat");
            assertThat(tomcat).isNotNull();
            @SuppressWarnings("unchecked")
            Map<String, Object> threads = (Map<String, Object>) tomcat.get("threads");
            assertThat(threads).isNotNull();
            assertThat(Integer.parseInt(String.valueOf(threads.get("max")))).isLessThanOrEqualTo(25);
            assertThat(Integer.parseInt(String.valueOf(threads.get("min-spare")))).isLessThanOrEqualTo(5);

            // Verify Hikari pool capped for 512MB RAM
            @SuppressWarnings("unchecked")
            Map<String, Object> spring = (Map<String, Object>) data.get("spring");
            @SuppressWarnings("unchecked")
            Map<String, Object> datasource = (Map<String, Object>) spring.get("datasource");
            @SuppressWarnings("unchecked")
            Map<String, Object> hikari = (Map<String, Object>) datasource.get("hikari");
            assertThat(String.valueOf(hikari.get("maximum-pool-size"))).contains(":8}");
            assertThat(String.valueOf(hikari.get("minimum-idle"))).contains(":2}");

            // Verify open-in-view is disabled
            @SuppressWarnings("unchecked")
            Map<String, Object> jpa = (Map<String, Object>) spring.get("jpa");
            assertThat(jpa.get("open-in-view")).isEqualTo(false);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read application-prod.yaml", e);
        }
    }
}
