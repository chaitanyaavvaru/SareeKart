package com.sareekart.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationInfoService;
import org.flywaydb.core.api.MigrationState;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TiDbCleanRoomSchemaParityTest {

    private static final String DEFAULT_TEST_DB = "sareekart_clean_room_test_db";
    private static String targetDb;
    private static String rootUrl;
    private static String targetUrl;
    private static String dbUser;
    private static String dbPass;
    private static boolean isDedicatedTestDb = false;

    private static final Set<String> EXPECTED_TABLES = Set.of(
        "ai_style_consultations",
        "approval_requests",
        "audit_logs",
        "cart_items",
        "carts",
        "categories",
        "colors",
        "conversations",
        "coupons",
        "customer_events",
        "fabrics",
        "graph_sync_failures",
        "inventory_items",
        "notifications",
        "occasions",
        "order_items",
        "orders",
        "password_reset_tokens",
        "pincode_overrides",
        "product_images",
        "products",
        "return_requests",
        "reviews",
        "stock_transfers",
        "trousseau_boards",
        "trousseau_ceremonies",
        "trousseau_collaborators",
        "trousseau_items",
        "trousseau_votes",
        "users",
        "visual_search_queries",
        "wallet_transactions",
        "wallets",
        "whatsapp_contacts",
        "whatsapp_messages",
        "whatsapp_notification_logs",
        "wishlists"
    );

    @BeforeAll
    static void setUpDatabase() throws Exception {
        String envUrl = System.getenv("SPRING_DATASOURCE_URL");
        String tidbHost = System.getenv("TIDB_HOST");

        if (tidbHost != null && !tidbHost.isBlank()) {
            String port = System.getenv("TIDB_PORT") != null ? System.getenv("TIDB_PORT") : "4000";
            targetDb = System.getenv("TIDB_DATABASE") != null ? System.getenv("TIDB_DATABASE") : "sareekart_db";
            dbUser = System.getenv("TIDB_USER") != null ? System.getenv("TIDB_USER") : "root";
            dbPass = System.getenv("TIDB_PASSWORD") != null ? System.getenv("TIDB_PASSWORD") : "";
            targetUrl = "jdbc:mysql://" + tidbHost + ":" + port + "/" + targetDb + "?useSSL=true&allowPublicKeyRetrieval=true&serverTimezone=UTC";
            rootUrl = "jdbc:mysql://" + tidbHost + ":" + port + "/?useSSL=true&allowPublicKeyRetrieval=true&serverTimezone=UTC";
            isDedicatedTestDb = false;
        } else if (envUrl != null && !envUrl.isBlank()) {
            if (envUrl.contains("db:3306")) {
                envUrl = envUrl.replace("db:3306", "localhost:3306");
            }
            targetUrl = envUrl;
            dbUser = System.getenv("SPRING_DATASOURCE_USERNAME") != null ? System.getenv("SPRING_DATASOURCE_USERNAME") : "root";
            dbPass = System.getenv("SPRING_DATASOURCE_PASSWORD") != null ? System.getenv("SPRING_DATASOURCE_PASSWORD") : "root123";
            targetDb = parseDatabaseName(targetUrl);
            rootUrl = "jdbc:mysql://localhost:3306/?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
            isDedicatedTestDb = false;

            if (targetUrl.contains("localhost:3306") || targetUrl.contains("127.0.0.1:3306")) {
                try (Connection rootConn = DriverManager.getConnection(rootUrl, dbUser, dbPass);
                     Statement stmt = rootConn.createStatement()) {
                    stmt.execute("CREATE DATABASE IF NOT EXISTS " + targetDb + " CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
                } catch (Exception ignored) {
                    // Non-fatal if root connection is restricted or user lacks CREATE DATABASE privilege
                }
            }
        } else {
            // Local clean-room MySQL execution
            targetDb = DEFAULT_TEST_DB;
            dbUser = System.getProperty("db.user", "root");
            dbPass = System.getProperty("db.password", "root123");
            rootUrl = "jdbc:mysql://localhost:3306/?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
            targetUrl = "jdbc:mysql://localhost:3306/" + targetDb + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
            isDedicatedTestDb = true;

            // Reset local clean-room database
            try (Connection rootConn = DriverManager.getConnection(rootUrl, dbUser, dbPass);
                 Statement stmt = rootConn.createStatement()) {
                stmt.execute("DROP DATABASE IF EXISTS " + targetDb);
                stmt.execute("CREATE DATABASE " + targetDb + " CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            }
        }
    }

    @AfterAll
    static void tearDownDatabase() throws Exception {
        if (isDedicatedTestDb) {
            try (Connection rootConn = DriverManager.getConnection(rootUrl, dbUser, dbPass);
                 Statement stmt = rootConn.createStatement()) {
                stmt.execute("DROP DATABASE IF EXISTS " + targetDb);
            }
        }
    }

    @Test
    @Order(1)
    @DisplayName("R1.1: Verify Flyway clean-room migration executes all 17 migrations (V1, V17-V32) with status SUCCESS")
    void testFlywayCleanRoomMigrationExecution() {
        Flyway flyway = Flyway.configure()
                .dataSource(targetUrl, dbUser, dbPass)
                .locations("classpath:db/migration")
                .baselineOnMigrate(false)
                .cleanDisabled(true)
                .validateOnMigrate(true)
                .load();

        flyway.migrate();

        MigrationInfoService infoService = flyway.info();
        MigrationInfo[] applied = infoService.applied();
        assertThat(applied)
                .withFailMessage("Expected exactly 17 migrations in Flyway history")
                .hasSize(17);

        List<String> versions = new ArrayList<>();
        for (MigrationInfo info : applied) {
            assertThat(info.getState())
                    .withFailMessage("Migration V" + info.getVersion() + " must have status SUCCESS")
                    .isEqualTo(MigrationState.SUCCESS);
            versions.add(info.getVersion().getVersion());
        }

        List<String> expectedVersions = List.of(
            "1", "17", "18", "19", "20", "21", "22", "23", "24", "25", "26", "27", "28", "29", "30", "31", "32"
        );
        assertThat(versions).containsExactlyElementsOf(expectedVersions);
    }

    @Test
    @Order(2)
    @DisplayName("R1.2: Verify 100% schema parity across all 37 application tables")
    void testThirtySevenApplicationTablesParity() throws Exception {
        try (Connection conn = DriverManager.getConnection(targetUrl, dbUser, dbPass);
             Statement stmt = conn.createStatement()) {

            Set<String> actualTables = new HashSet<>();
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT table_name FROM information_schema.tables " +
                    "WHERE table_schema = '" + targetDb + "' AND table_type = 'BASE TABLE' AND table_name != 'flyway_schema_history'")) {
                while (rs.next()) {
                    actualTables.add(rs.getString(1).toLowerCase());
                }
            }

            assertThat(actualTables)
                    .withFailMessage("Expected exactly 37 application tables")
                    .hasSize(37);
            assertThat(actualTables)
                    .withFailMessage("Table names do not match expected schema catalog")
                    .containsExactlyInAnyOrderElementsOf(EXPECTED_TABLES);
        }
    }

    @Test
    @Order(3)
    @DisplayName("R1.3: Verify 100% schema parity across all 368 application columns")
    void testThreeHundredSixtyEightColumnsParity() throws Exception {
        try (Connection conn = DriverManager.getConnection(targetUrl, dbUser, dbPass);
             Statement stmt = conn.createStatement()) {

            int columnCount = 0;
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT count(*) FROM information_schema.columns " +
                    "WHERE table_schema = '" + targetDb + "' AND table_name != 'flyway_schema_history'")) {
                if (rs.next()) {
                    columnCount = rs.getInt(1);
                }
            }

            assertThat(columnCount)
                    .withFailMessage("Expected exactly 368 columns across all 37 application tables, found " + columnCount)
                    .isEqualTo(368);
        }
    }

    @Test
    @Order(4)
    @DisplayName("R1.4: Verify Hibernate ddl-auto=validate succeeds without schema mutation or validation errors")
    void testHibernateDdlAutoValidateStrictCompliance() {
        org.springframework.jdbc.datasource.DriverManagerDataSource ds =
                new org.springframework.jdbc.datasource.DriverManagerDataSource();
        ds.setDriverClassName("com.mysql.cj.jdbc.Driver");
        ds.setUrl(targetUrl);
        ds.setUsername(dbUser);
        ds.setPassword(dbPass);

        org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean emfBean =
                new org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean();
        emfBean.setDataSource(ds);
        emfBean.setPackagesToScan("com.sareekart.entity");

        org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter vendorAdapter =
                new org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter();
        emfBean.setJpaVendorAdapter(vendorAdapter);

        Map<String, Object> jpaProperties = new HashMap<>();
        jpaProperties.put("hibernate.hbm2ddl.auto", "validate");
        jpaProperties.put("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
        jpaProperties.put("hibernate.physical_naming_strategy", "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy");
        jpaProperties.put("hibernate.implicit_naming_strategy", "org.springframework.boot.orm.jpa.hibernate.SpringImplicitNamingStrategy");
        emfBean.setJpaPropertyMap(jpaProperties);

        emfBean.afterPropertiesSet();
        jakarta.persistence.EntityManagerFactory emf = emfBean.getObject();
        assertThat(emf).isNotNull();
        assertThat(emf.isOpen()).isTrue();
        emf.close();
    }

    static String parseDatabaseName(String jdbcUrl) {
        if (jdbcUrl == null || jdbcUrl.isBlank()) {
            return DEFAULT_TEST_DB;
        }
        try {
            int schemeIdx = jdbcUrl.indexOf("//");
            if (schemeIdx == -1) {
                return DEFAULT_TEST_DB;
            }
            String afterScheme = jdbcUrl.substring(schemeIdx + 2);
            int slashIdx = afterScheme.indexOf('/');
            if (slashIdx == -1) {
                return DEFAULT_TEST_DB;
            }
            String path = afterScheme.substring(slashIdx + 1);
            int queryIdx = path.indexOf('?');
            if (queryIdx != -1) {
                path = path.substring(0, queryIdx);
            }
            int semiIdx = path.indexOf(';');
            if (semiIdx != -1) {
                path = path.substring(0, semiIdx);
            }
            path = path.trim();
            if (path.endsWith("/")) {
                path = path.substring(0, path.length() - 1);
            }
            return path.isEmpty() ? DEFAULT_TEST_DB : path;
        } catch (Exception e) {
            return DEFAULT_TEST_DB;
        }
    }
}
