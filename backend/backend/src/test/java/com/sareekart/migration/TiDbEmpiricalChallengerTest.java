package com.sareekart.migration;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceException;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationInfoService;
import org.flywaydb.core.api.MigrationState;
import org.hibernate.tool.schema.spi.SchemaManagementException;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TiDbEmpiricalChallengerTest {

    private static final String CHALLENGER_DB = "sareekart_challenger_parity_db";
    private static String rootUrl;
    private static String targetUrl;
    private static String dbUser;
    private static String dbPass;

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
    static void setUpCleanRoomEnvironment() throws Exception {
        dbUser = System.getProperty("db.user", "root");
        dbPass = System.getProperty("db.password", "root123");
        rootUrl = "jdbc:mysql://localhost:3306/?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        targetUrl = "jdbc:mysql://localhost:3306/" + CHALLENGER_DB + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

        // Drop and recreate completely fresh database
        try (Connection rootConn = DriverManager.getConnection(rootUrl, dbUser, dbPass);
             Statement stmt = rootConn.createStatement()) {
            stmt.execute("DROP DATABASE IF EXISTS " + CHALLENGER_DB);
            stmt.execute("CREATE DATABASE " + CHALLENGER_DB + " CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
        }
    }

    @AfterAll
    static void tearDownCleanRoomEnvironment() throws Exception {
        try (Connection rootConn = DriverManager.getConnection(rootUrl, dbUser, dbPass);
             Statement stmt = rootConn.createStatement()) {
            stmt.execute("DROP DATABASE IF EXISTS " + CHALLENGER_DB);
        }
    }

    private LocalContainerEntityManagerFactoryBean createEntityManagerFactoryBean() {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName("com.mysql.cj.jdbc.Driver");
        ds.setUrl(targetUrl);
        ds.setUsername(dbUser);
        ds.setPassword(dbPass);

        LocalContainerEntityManagerFactoryBean emfBean = new LocalContainerEntityManagerFactoryBean();
        emfBean.setDataSource(ds);
        emfBean.setPackagesToScan("com.sareekart.entity");

        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        emfBean.setJpaVendorAdapter(vendorAdapter);

        Map<String, Object> jpaProperties = new HashMap<>();
        jpaProperties.put("hibernate.hbm2ddl.auto", "validate");
        jpaProperties.put("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
        jpaProperties.put("hibernate.physical_naming_strategy", "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy");
        jpaProperties.put("hibernate.implicit_naming_strategy", "org.springframework.boot.orm.jpa.hibernate.SpringImplicitNamingStrategy");
        emfBean.setJpaPropertyMap(jpaProperties);
        return emfBean;
    }

    @Test
    @Order(1)
    @DisplayName("Challenger Test 1: Clean-room Flyway baseline execution across empty DB")
    void testFlywayCleanRoomMigration() {
        Flyway flyway = Flyway.configure()
                .dataSource(targetUrl, dbUser, dbPass)
                .locations("classpath:db/migration")
                .baselineOnMigrate(false)
                .cleanDisabled(true)
                .validateOnMigrate(true)
                .load();

        int migrationsApplied = flyway.migrate().migrationsExecuted;
        assertThat(migrationsApplied)
                .withFailMessage("Flyway must apply all 17 migrations from clean-room")
                .isEqualTo(17);

        MigrationInfoService infoService = flyway.info();
        MigrationInfo[] applied = infoService.applied();
        assertThat(applied).hasSize(17);

        for (MigrationInfo info : applied) {
            assertThat(info.getState())
                    .withFailMessage("Migration V%s must be SUCCESS", info.getVersion())
                    .isEqualTo(MigrationState.SUCCESS);
        }
    }

    @Test
    @Order(2)
    @DisplayName("Challenger Test 2: Direct SQL verification of exactly 37 tables and 368 columns")
    void testDirectSqlTableAndColumnCounts() throws Exception {
        try (Connection conn = DriverManager.getConnection(targetUrl, dbUser, dbPass);
             Statement stmt = conn.createStatement()) {

            Set<String> actualTables = new HashSet<>();
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT table_name FROM information_schema.tables " +
                    "WHERE table_schema = '" + CHALLENGER_DB + "' AND table_type = 'BASE TABLE' AND table_name != 'flyway_schema_history'")) {
                while (rs.next()) {
                    actualTables.add(rs.getString(1).toLowerCase());
                }
            }

            assertThat(actualTables)
                    .withFailMessage("Direct query must find strictly 37 tables")
                    .hasSize(37);
            assertThat(actualTables)
                    .withFailMessage("Direct query table catalog mismatch")
                    .containsExactlyInAnyOrderElementsOf(EXPECTED_TABLES);

            int columnCount = 0;
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT count(*) FROM information_schema.columns " +
                    "WHERE table_schema = '" + CHALLENGER_DB + "' AND table_name != 'flyway_schema_history'")) {
                if (rs.next()) {
                    columnCount = rs.getInt(1);
                }
            }

            assertThat(columnCount)
                    .withFailMessage("Direct query must find strictly 368 columns across 37 tables, found %d", columnCount)
                    .isEqualTo(368);
        }
    }

    @Test
    @Order(3)
    @DisplayName("Challenger Test 3: Direct SQL audit of flyway_schema_history ledger")
    void testDirectSqlFlywayHistoryAudit() throws Exception {
        try (Connection conn = DriverManager.getConnection(targetUrl, dbUser, dbPass);
             Statement stmt = conn.createStatement()) {

            int totalRows = 0;
            int successfulRows = 0;
            List<String> recordedVersions = new ArrayList<>();

            try (ResultSet rs = stmt.executeQuery(
                    "SELECT version, success FROM " + CHALLENGER_DB + ".flyway_schema_history ORDER BY installed_rank ASC")) {
                while (rs.next()) {
                    totalRows++;
                    String version = rs.getString("version");
                    boolean success = rs.getBoolean("success");
                    recordedVersions.add(version);
                    if (success) {
                        successfulRows++;
                    }
                }
            }

            assertThat(totalRows)
                    .withFailMessage("Expected exactly 17 entries in flyway_schema_history")
                    .isEqualTo(17);
            assertThat(successfulRows)
                    .withFailMessage("All 17 entries in flyway_schema_history must have success=1")
                    .isEqualTo(17);

            List<String> expectedVersions = List.of(
                "1", "17", "18", "19", "20", "21", "22", "23", "24", "25", "26", "27", "28", "29", "30", "31", "32"
            );
            assertThat(recordedVersions).containsExactlyElementsOf(expectedVersions);
        }
    }

    @Test
    @Order(4)
    @DisplayName("Challenger Test 4: Positive Hibernate validation with intact schema")
    void testHibernateValidationPositiveCase() {
        LocalContainerEntityManagerFactoryBean emfBean = createEntityManagerFactoryBean();
        emfBean.afterPropertiesSet();
        EntityManagerFactory emf = emfBean.getObject();
        assertThat(emf).isNotNull();
        assertThat(emf.isOpen()).isTrue();
        emf.close();
    }

    @Test
    @Order(5)
    @DisplayName("Challenger Test 5: Negative Hibernate validation on missing table (adversarial)")
    void testHibernateValidationNegativeMissingTable() throws Exception {
        // Adversarially hide the 'return_requests' table
        try (Connection conn = DriverManager.getConnection(targetUrl, dbUser, dbPass);
             Statement stmt = conn.createStatement()) {
            stmt.execute("RENAME TABLE " + CHALLENGER_DB + ".return_requests TO " + CHALLENGER_DB + ".return_requests_hidden");
        }

        try {
            LocalContainerEntityManagerFactoryBean emfBean = createEntityManagerFactoryBean();
            assertThatThrownBy(emfBean::afterPropertiesSet)
                    .withFailMessage("Hibernate must reject startup when table 'return_requests' is missing")
                    .isInstanceOf(PersistenceException.class)
                    .hasRootCauseInstanceOf(SchemaManagementException.class)
                    .hasMessageContaining("missing table [return_requests]");
        } finally {
            // Restore table
            try (Connection conn = DriverManager.getConnection(targetUrl, dbUser, dbPass);
                 Statement stmt = conn.createStatement()) {
                stmt.execute("RENAME TABLE " + CHALLENGER_DB + ".return_requests_hidden TO " + CHALLENGER_DB + ".return_requests");
            }
        }

        // Verify restoration allows clean validation again
        LocalContainerEntityManagerFactoryBean restoredEmfBean = createEntityManagerFactoryBean();
        restoredEmfBean.afterPropertiesSet();
        EntityManagerFactory restoredEmf = restoredEmfBean.getObject();
        assertThat(restoredEmf).isNotNull();
        assertThat(restoredEmf.isOpen()).isTrue();
        restoredEmf.close();
    }

    @Test
    @Order(6)
    @DisplayName("Challenger Test 6: Negative Hibernate validation on missing column (adversarial)")
    void testHibernateValidationNegativeMissingColumn() throws Exception {
        // Adversarially drop column 'tracking_number' from 'orders'
        try (Connection conn = DriverManager.getConnection(targetUrl, dbUser, dbPass);
             Statement stmt = conn.createStatement()) {
            stmt.execute("ALTER TABLE " + CHALLENGER_DB + ".orders DROP COLUMN tracking_number");
        }

        try {
            LocalContainerEntityManagerFactoryBean emfBean = createEntityManagerFactoryBean();
            assertThatThrownBy(emfBean::afterPropertiesSet)
                    .withFailMessage("Hibernate must reject startup when column 'tracking_number' is missing from 'orders'")
                    .isInstanceOf(PersistenceException.class)
                    .hasRootCauseInstanceOf(SchemaManagementException.class)
                    .hasMessageContaining("missing column [tracking_number] in table [orders]");
        } finally {
            // Restore column
            try (Connection conn = DriverManager.getConnection(targetUrl, dbUser, dbPass);
                 Statement stmt = conn.createStatement()) {
                stmt.execute("ALTER TABLE " + CHALLENGER_DB + ".orders ADD COLUMN tracking_number VARCHAR(100) NULL AFTER status");
            }
        }

        // Verify restoration allows clean validation again
        LocalContainerEntityManagerFactoryBean restoredEmfBean = createEntityManagerFactoryBean();
        restoredEmfBean.afterPropertiesSet();
        EntityManagerFactory restoredEmf = restoredEmfBean.getObject();
        assertThat(restoredEmf).isNotNull();
        assertThat(restoredEmf.isOpen()).isTrue();
        restoredEmf.close();
    }
}
