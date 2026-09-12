# SareeKart Backend Architecture & Test Suite Survey Report

**Date**: 2026-09-03  
**Auditor**: teamwork_preview_explorer_survey_1  
**Target Repository**: `/Users/chaitanyachaitu/Downloads/SareeKart-main`  
**Working Directory**: `/Users/chaitanyachaitu/Downloads/SareeKart-main/.agents/teamwork_preview_explorer_survey_1`  
**Execution Context**: Local Offline MacOS Environment (Darwin aarch64, Java 17.0.18, Maven 3.9.16)

---

## 1. Executive Summary

A comprehensive architectural and operational survey of the SareeKart backend was executed to evaluate codebase structure, build tooling, automated test readiness, database configuration, health check readiness, and offline boundary compliance.

### Key Highlights:
1. **Location & Layout**: The active backend project resides in `backend/backend/` (doubly nested). Root-level `backend/src` and `controller/` are orphaned/legacy stubs and do not participate in compilation.
2. **Spring Boot & Java**: Built on **Spring Boot 3.5.15** with **Spring Framework 6.2.19** and compiled targeting **Java 17**.
3. **Maven Wrapper**: `./mvnw` is present and functional (Apache Maven 3.9.16).
4. **Test Suite Status**: `./mvnw test` runs cleanly. **18 tests executed across 6 test classes with 0 failures, 0 errors, 0 skipped** (Execution time: ~8.4s).
5. **Offline Capability**: `./mvnw test -o` (strictly offline flag) passed 100%, confirming that all required plugins and dependencies are fully cached locally in `~/.m2`.
6. **Database & Migrations**: Configured for **MySQL on port 3306** (`sareekart_db`). Flyway is **not present** (0 references across the repository). Schema evolution is driven by Hibernate (`spring.jpa.hibernate.ddl-auto: update`), and baseline seed data is loaded via `DataSeeder` (`CommandLineRunner`). In-memory H2 database is configured under the `test` profile (`application-test.yaml`) for isolated test execution.
7. **Port Bindings**: Port 3307 is **not used** in this project (port 3307 was referenced in user docs for a separate project directory). Active service binds: MySQL on `3306`, Backend API on `8081`, Frontend Vite dev server on `5173`.
8. **Health Check Readiness**: `http://localhost:8081/api/products` is actively running, unauthenticated (`permitAll()`), returning `HTTP/1.1 200 OK` with 11 pre-seeded saree products.
9. **Offline Boundary Compliance**: Zero external calls or credentials required for local tests. Outbound clients (Razorpay, WhatsApp API, Spring AI / OpenAI) gracefully handle missing/dummy keys without throwing fatal initialization exceptions.

---

## 2. Codebase Layout & Architectural Structure

### Directory Tree Overview
```
/Users/chaitanyachaitu/Downloads/SareeKart-main/
├── backend/
│   ├── backend/                     <-- PRIMARY ACTIVE BACKEND PROJECT ROOT
│   │   ├── .mvn/wrapper/            <-- Maven wrapper properties
│   │   ├── mvnw, mvnw.cmd           <-- Maven wrapper executables
│   │   ├── pom.xml                  <-- Project Object Model definition
│   │   ├── Dockerfile               <-- Backend container build spec
│   │   ├── target/                  <-- Build output artifacts
│   │   └── src/
│   │       ├── main/
│   │       │   ├── java/
│   │       │   │   ├── com/example/backend/  <-- Deprecated stubs
│   │       │   │   │   ├── BackendApplication.java (deprecated placeholder)
│   │       │   │   │   └── controller/ProductController.java (deprecated placeholder)
│   │       │   │   └── com/sareekart/        <-- Active production package
│   │       │   │       ├── SareeKartApplication.java (Main entrypoint)
│   │       │   │       ├── config/           <-- App configs & seeders
│   │       │   │       ├── controller/       <-- REST & WebSocket controllers
│   │       │   │       ├── dto/              <-- Request, Response & Webhook DTOs
│   │       │   │       ├── entity/           <-- JPA entities
│   │       │   │       ├── exception/        <-- Exception handlers & custom errors
│   │       │   │       ├── mapper/           <-- Entity <-> DTO mappers
│   │       │   │       ├── repository/       <-- Spring Data JPA interfaces
│   │       │   │       ├── security/         <-- JWT & UserDetailsService
│   │       │   │       └── service/          <-- Business logic & implementations
│   │       │   └── resources/
│   │       │       └── application.yaml      <-- Main configuration file
│   │       └── test/
│   │           ├── java/
│   │           │   ├── com/example/backend/  <-- Context boot integration test
│   │           │   └── com/sareekart/        <-- Unit and slice tests
│   │           └── resources/
│   │               ├── application-test.yaml <-- In-memory H2 test configuration
│   │               └── mockito-extensions/   <-- MockMaker configuration
│   └── src/                         <-- Orphaned duplicate/legacy directory (ignored by build)
├── controller/                      <-- Orphaned ProductController.java (ignored by build)
├── docker-compose.yml               <-- Stack definition (db:3306, backend:8081, frontend:80)
├── manage.sh                        <-- Local service management script
└── README.md                        <-- Operational documentation
```

### Key Entry Points & Component Roles
- **Application Class**: `com.sareekart.SareeKartApplication`
  - Annotations: `@SpringBootApplication(exclude = {org.springframework.cloud.function.context.config.ContextFunctionCatalogAutoConfiguration.class})`, `@EnableJpaAuditing`.
- **Controllers**:
  - `ProductController`: Public catalog endpoints (`/api/products`, `/api/products/{id}`, `/api/products/search`, `/api/products/filter`) + admin mutations (`/api/admin/products`).
  - `AuthController`: User authentication and registration (`/api/auth/register`, `/api/auth/login`, `/api/auth/profile`).
  - `CartController`: Customer cart operations (`/api/cart/**`).
  - `CategoryController`: Public category listing (`/api/categories/**`).
  - `OrderController`: Customer order placement and tracking (`/api/orders/**`).
  - `PaymentController`: Razorpay order creation and webhook verification (`/api/payment/**`).
  - `AdminController`: Analytics and administrative telemetry (`/api/admin/dashboard`).
  - `CouponController`: Promo discount calculation (`/api/coupons/**`).
  - `ReviewController`: Product rating and feedback (`/api/reviews/**`).
  - `WishlistController`: Customer saved items (`/api/wishlist/**`).
  - `WhatsAppWebhookController`: Meta webhook verification and inbound message reception (`/api/webhook/whatsapp`).
  - `ChatController`: STOMP/WebSocket admin chat handling (`/chat.sendMessage`).

---

## 3. Build Tooling, Java/Spring Boot Versions, and Dependencies

### Environment Matrix
| Property | Value | Evidence |
| :--- | :--- | :--- |
| **Java Version Target** | 17 | `backend/backend/pom.xml:30` (`<java.version>17</java.version>`) |
| **Active JVM** | 17.0.18 (Homebrew) | `openjdk 17.0.18 2026-01-20`, `/opt/homebrew/Cellar/openjdk@17/17.0.18` |
| **Spring Boot Version** | 3.5.15 | `backend/backend/pom.xml:8` (`<version>3.5.15</version>`) |
| **Spring Framework** | 6.2.19 | Runtime log: `Running with Spring Boot v3.5.15, Spring v6.2.19` |
| **Maven Version** | 3.9.16 | Bundled wrapper: `apache-maven-3.9.16` |
| **Packaging** | JAR | `sareekart-0.0.1-SNAPSHOT.jar` |

### Core Dependencies in `pom.xml`
1. **Spring Starters**:
   - `spring-boot-starter-data-jpa`
   - `spring-boot-starter-validation`
   - `spring-boot-starter-web`
   - `spring-boot-starter-security`
   - `spring-boot-starter-websocket`
   - `spring-boot-starter-webflux`
2. **Database & Drivers**:
   - `com.mysql:mysql-connector-j` (runtime)
   - `com.h2database:h2` (test scope)
3. **Security & JWT**:
   - `io.jsonwebtoken:jjwt-api:0.12.6`, `jjwt-impl`, `jjwt-jackson`
4. **Documentation**:
   - `org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.8` (OpenAPI 3 / Swagger)
5. **Integrations**:
   - `com.razorpay:razorpay-java:1.4.7`
   - `org.springframework.ai:spring-ai-openai-spring-boot-starter:1.0.0-M1`
6. **Code Generation**:
   - `org.projectlombok:lombok` (configured with annotation processor in `maven-compiler-plugin`)
7. **Test Frameworks**:
   - `spring-boot-starter-test` (JUnit 5 Jupiter, Mockito, AssertJ, Spring Test)
   - `spring-security-test`

---

## 4. Backend Test Suite Analysis & Execution Results

### Test Classes & Coverage Inventory
The backend test suite consists of 6 test classes spanning unit tests, web layer MockMvc tests, and full context integration tests:

| Test Class | Package | Type | Tests | Key Invariants Verified |
| :--- | :--- | :--- | :---: | :--- |
| **`CorsConfigTest`** | `com.sareekart.config` | Unit / MockFilter | 2 | Allows preflight from `http://127.0.0.1:5173`; returns HTTP 403 for unauthorized origins (`http://malicious.example`). |
| **`RegistrationFlowTest`** | `com.sareekart.controller` | MockMvc Web Slice | 3 | Valid user registration returns HTTP 201 with JWT token; duplicate email yields HTTP 400; non-digit mobile number fails Bean Validation with descriptive error message. |
| **`CartServiceTest`** | `com.sareekart.service` | Unit (Mockito) | 2 | Throws `BadRequestException` and suppresses save when requested cart item quantity exceeds available stock; enforces stock limit during cart item quantity update. |
| **`ProductServiceTest`** | `com.sareekart.service` | Unit (Mockito) | 9 | Paged product listing; retrieval by ID (success); `ResourceNotFoundException` on missing ID; creation with valid category; creation rejection with invalid category; update product details; soft deletion flag toggle (`active: false`); product keyword search; category/price/fabric filter queries. |
| **`UserServiceRegistrationTest`** | `com.sareekart.service` | Unit (Mockito) | 1 | Trims and normalizes whitespace in names, emails, and phone numbers during user registration; validates password encryption and token generation. |
| **`BackendApplicationTests`** | `com.example.backend` | Integration (`@SpringBootTest`) | 1 | Verifies complete Spring Boot `ApplicationContext` bootstrapping, JPA EntityManagerFactory creation, H2 schema generation, Spring Security filter chains, and STOMP message broker initialization under `@ActiveProfiles("test")`. |

### Execution Verification Commands & Results
```bash
# Standard test run
cd /Users/chaitanyachaitu/Downloads/SareeKart-main/backend/backend
./mvnw test
```
**Output Summary**:
```
[INFO] Results:
[INFO] 
[INFO] Tests run: 18, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  8.464 s
```

```bash
# Offline verification run
./mvnw test -o
```
**Output Summary**:
```
[INFO] Results:
[INFO] 
[INFO] Tests run: 18, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  8.432 s
```

### Diagnostics & Compiler Warning Evaluation
During test compilation and execution, the following warnings were observed and audited:
1. **Duplicate JSONObject Warning**:
   - `WARN: Found multiple occurrences of org.json.JSONObject on the class path` (from `com.vaadin.external.google.android-json` pulled by `spring-boot-starter-test` vs `org.json:json:20231013` pulled by `razorpay-java`).
   - *Impact*: Benign in testing, but can be cleaned up by excluding `android-json` from `spring-boot-starter-test` if strict zero-warning policy is enforced.
2. **Hibernate Dialect Deprecation**:
   - `HHH90000025: H2Dialect does not need to be specified explicitly using 'hibernate.dialect'`.
   - *Impact*: Benign informational warning; Hibernate 6 automatically detects H2.
3. **Netty macOS DNS Resolution**:
   - `ERROR: Unable to load io.netty.resolver.dns.macos.MacOSDnsServerAddressStreamProvider, fallback to system defaults.`
   - *Impact*: Benign fallback to standard Java system DNS; does not affect test execution.
4. **Razorpay Key Notice**:
   - `WARN: Razorpay is not configured. Set RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET before accepting online payments.`
   - *Impact*: Expected and intentional when running tests without live merchant API credentials.

---

## 5. Database Configuration, Migrations, Ports, & H2 Test Profiles

### Database Architecture & Port Bindings
A detailed investigation of the repository configuration resolved discrepancies between user-level notes and repository-specific configuration:

| Component | Repository Value | Active Local State | Notes |
| :--- | :--- | :--- | :--- |
| **MySQL Port** | **3306** | `mysqld` PID 871 listening on `0.0.0.0:3306` / `[::]:3306` | Configured in `application.yaml` (`localhost:3306`), `docker-compose.yml` (`3306:3306`), and `README.md`. |
| **Port 3307** | *Not Used* | No process listening on 3307 | Port 3307 was mentioned in `~/AGENTS.md` for a different sibling project (`~/SareeKart`), but is **not referenced or bound** in `Downloads/SareeKart-main`. |
| **Database Name** | `sareekart_db` | Created and populated | Auto-created via JDBC parameter `createDatabaseIfNotExist=true`. |
| **Default User** | `root` | Authenticated | Default root user with password `root123`. |

### Database Schema Evolution: Hibernate vs. Flyway
- **Flyway Status**: Flyway is **NOT used** in this project.
  - A recursive grep across the codebase for `flyway` returned 0 occurrences.
  - No `flyway-core` or `flyway-mysql` dependency exists in `pom.xml`.
  - No `src/main/resources/db/migration` directory exists.
- **Active Schema Mechanism**:
  - Hibernate DDL auto-update (`spring.jpa.hibernate.ddl-auto: update`) dynamically maintains the relational schema against JPA entity annotations.
  - Audit timestamps (`createdAt`, `updatedAt`) are managed by `@EnableJpaAuditing` and Spring Data JPA.
- **Baseline Data Seeding**:
  - Implemented in `com.sareekart.config.DataSeeder` (`CommandLineRunner`).
  - Checks if table counts are zero before inserting initial records:
    - Admin: `admin@sareekart.com` / `admin123` (`ROLE_ADMIN`)
    - Customer: `customer@sareekart.com` / `customer123` (`ROLE_CUSTOMER`)
    - 8 saree categories (`Silk Sarees`, `Cotton Sarees`, `Banarasi Sarees`, etc.)
    - 12 initial products with pricing, stock quantity, and imagery
    - 2 promotional coupons (`WELCOME10` - 10%, `WEDDING20` - 20%)

### H2 Test Profile Configuration
The test execution environment uses an isolated in-memory H2 instance configured in `backend/backend/src/test/resources/application-test.yaml`:
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:sareekart_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    username: sa
    password:
    driver-class-name: org.h2.Driver

  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: false
```
**Benefits**:
- Zero coupling to external or host-level MySQL daemons during test execution.
- Tests execute with clean schema creation (`ddl-auto: create-drop`) and teardown per test suite run.
- High execution speed (<3s for full Spring Boot test context lifecycle).

---

## 6. Health Check Endpoint Readiness

### Server Port Binding
- Backend port: **8081** (defined in `application.yaml:27` via `server.port: 8081`).
- Active state: Java process PID 6259 is currently listening on port 8081.

### Public Endpoint Inspection: `/api/products`
- **Security Rule**: Configured in `SecurityConfig.java:66`:
  ```java
  .requestMatchers(HttpMethod.GET, "/api/products/**", "/api/categories/**").permitAll()
  ```
  `GET` requests to `/api/products` are strictly public and do not require Authorization bearer tokens.
- **Live HTTP Verification**:
  ```bash
  curl -s -i http://localhost:8081/api/products
  ```
  **Response Received**:
  - Status: `HTTP/1.1 200 OK`
  - Content-Type: `application/json`
  - Payload structure:
    ```json
    {
      "success": true,
      "data": {
        "content": [
          {
            "id": 11,
            "name": "Taranga Kanchi Silk Brocade Green Saree",
            "price": 17650.00,
            "categoryId": 6,
            "categoryName": "Kanchipuram Sarees",
            "stockQuantity": 9,
            "active": true
          }
          // ... 11 items returned
        ],
        "page": 0,
        "size": 12,
        "totalElements": 11,
        "totalPages": 1,
        "last": true
      },
      "timestamp": "2026-09-03T15:56:47.808498"
    }
    ```
- **Readiness Verdict**: The `/api/products` endpoint is **fully functional, verified 200 OK**, and returns populated catalog data.

---

## 7. Security, Authentication, & Access Control Architecture

### Authentication Architecture
- **Stateless JWT Flow**:
  - Filter: `JwtAuthenticationFilter` intercepts requests, extracts Bearer tokens, validates signatures via `JwtTokenProvider` using key `SareeKartSecretKey2024ForJWTTokenGenerationAndValidation`, and sets Spring Security context.
  - User Details: Loaded via `CustomUserDetailsService` querying `UserRepository.findByEmail(...)`.
  - Passwords: Encrypted with `BCryptPasswordEncoder`.
- **Authorization Matrix**:
  | Route / Path Pattern | HTTP Method | Permitted Roles | Notes |
  | :--- | :---: | :---: | :--- |
  | `/api/auth/**` | ANY | Public (`permitAll()`) | Registration, login, token issuance |
  | `/api/products/**` | GET | Public (`permitAll()`) | Catalog discovery |
  | `/api/categories/**` | GET | Public (`permitAll()`) | Category listing |
  | `/api/webhook/whatsapp/**` | ANY | Public (`permitAll()`) | Inbound Meta webhooks |
  | `/ws-sareekart/**` | ANY | Public (`permitAll()`) | WebSocket handshake |
  | `/swagger-ui/**`, `/v3/api-docs/**` | ANY | Public (`permitAll()`) | OpenAPI documentation |
  | `/api/admin/**` | ANY | `ADMIN` only (`hasRole('ADMIN')`) | Admin dashboard, inventory updates |
  | *All other endpoints* | ANY | Authenticated (`authenticated()`) | Cart, orders, user profile, wishlist |

---

## 8. Offline Boundary & Third-Party Integration Audit

### Local Network Isolation
- All services and connectors strictly bind to local interfaces:
  - Frontend: `http://localhost:5173`
  - Backend: `http://localhost:8081`
  - Database: `localhost:3306`
- Zero outbound deployment scripts or CI cloud syncing mechanisms are invoked during tests.

### Third-Party Outbound Integrations
1. **Razorpay Gateway** (`PaymentServiceImpl`, `PaymentController`):
   - Initializes conditionally only if `RAZORPAY_KEY_ID` and `RAZORPAY_KEY_SECRET` are provided.
   - When keys are unset or empty, logs a warning and prevents outbound network requests.
2. **WhatsApp Cloud API** (`WhatsAppApiClient`):
   - Verifies `apiToken` and `phoneNumberId`.
   - If missing or dummy, logs a warning and immediately returns without attempting HTTP network dispatches.
   - Any reactive WebClient errors are safely suppressed via `onErrorResume(e -> Mono.empty())`.
3. **OpenAI / Spring AI** (`AIChatbotService`, `AIChatClientConfig`):
   - Uses placeholder key `sk-proj-dummy-key`.
   - Calls to `chatClient.prompt().call()` are fully wrapped in a `try-catch (Exception e)` block that falls back to a friendly error message without throwing unhandled exceptions.
4. **Maven Dependency Resolution**:
   - Tested with `./mvnw test -o`.
   - Confirmed 100% offline executable.

---

## 9. Synthesis & Key Takeaways for Downstream Verification Tracks

| Area | Current State | Downstream Consideration |
| :--- | :--- | :--- |
| **Backend Location** | `backend/backend/` | Commands must execute with `cd backend/backend` or `manage.sh`. Root-level `backend/src` can be ignored. |
| **Automated Tests** | 18 passing tests (0 failures) | Baseline backend test requirement (R1) is satisfied and stable. |
| **Database Port** | Port 3306 is authoritative | Any checklists referencing port 3307 should note that 3306 is the actual configured port for this repository. |
| **Database Migrations** | Hibernate `ddl-auto: update` | If formal migration tracking (Flyway) is audited under checklist requirements, note that the project intentionally relies on JPA DDL updates + `DataSeeder`. |
| **Health Check** | `/api/products` returns 200 OK | Health check requirement on port 8081 is satisfied. |
| **Offline Safety** | 100% offline compliant | Build, test, and run do not trigger outbound external requests or leak credentials. |

---
*Report compiled by teamwork_preview_explorer_survey_1. End of survey report.*
