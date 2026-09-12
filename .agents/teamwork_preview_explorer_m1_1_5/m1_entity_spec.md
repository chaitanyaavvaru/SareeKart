# Milestone 1 Technical Specification: Backend Domain Model & Persistence

**Document Author**: Explorer 1 (Backend Domain Model & Persistence)  
**Working Directory**: `/Users/chaitanyachaitu/Downloads/SareeKart-main/.agents/teamwork_preview_explorer_m1_1_5`  
**Target Project**: SareeKart v3.0 Module 1 (Customer Returns & Exchanges)  
**Date**: 2026-09-11  

---

## 1. Executive Summary & Architecture Context

Milestone 1 introduces the backend domain model, persistence layer, Flyway schema migration, repository queries, and service foundation for self-service customer returns and exchanges.

### 1.1 Technical Stack & Conventions
- **Framework**: Spring Boot 3.5.15 on Java 17.
- **ORM / Persistence**: Spring Data JPA with Hibernate ORM 6.6.
- **Database Environments**:
  - Runtime: MySQL 8.0/8.4 running on port 3306 (`sareekart_db`), configured with Hibernate `ddl-auto: update` in `application.yaml`.
  - Testing: H2 in-memory DB (`MODE=MySQL;DATABASE_TO_LOWER=TRUE`) with `ddl-auto: create-drop` in `src/test/resources/application-test.yaml`.
- **Migration Engine**: Flyway migration sequence appended sequentially at `src/main/resources/db/migration/V17__create_return_requests_table.sql`, building upon SareeKart reference migrations `V1`–`V16`.
- **Code Placement & Packages**:
  - Entities & Enums: `com.sareekart.entity`
  - Converters: `com.sareekart.converter` (or within `com.sareekart.entity`)
  - Repositories: `com.sareekart.repository`
  - DTOs: `com.sareekart.dto.request` and `com.sareekart.dto.response`
  - Services: `com.sareekart.service` and `com.sareekart.service.impl`
  - Controllers: `com.sareekart.controller`
  - Migrations: `backend/backend/src/main/resources/db/migration/`

---

## 2. Enums Specification

All enums must be placed in `com.sareekart.entity` to align with the existing project convention where all domain enums (`OrderStatus`, `Role`, `DeliveryStatus`, `ConversationStatus`, `MessageType`, `SenderType`) reside directly in `com.sareekart.entity`.

### 2.1 `ReturnStatus.java`
- **File Path**: `backend/backend/src/main/java/com/sareekart/entity/ReturnStatus.java`
- **Package**: `com.sareekart.entity`
- **Code**:
```java
package com.sareekart.entity;

public enum ReturnStatus {
    PENDING,
    APPROVED,
    PICKUP_SCHEDULED,
    REJECTED,
    COMPLETED
}
```
- **Lifecycle Semantics**:
  - `PENDING`: Initial state upon customer submission.
  - `APPROVED`: Claim approved by staff (`OWNER`, `MANAGER`, `ADMIN`).
  - `PICKUP_SCHEDULED`: Reverse logistics courier and AWB tracking number assigned.
  - `REJECTED`: Claim rejected by staff with mandatory explanatory `adminNotes`. Terminal state.
  - `COMPLETED`: Doorstep item received and refund processed or exchange fulfilled. Terminal state.

---

### 2.2 `ReturnType.java`
- **File Path**: `backend/backend/src/main/java/com/sareekart/entity/ReturnType.java`
- **Package**: `com.sareekart.entity`
- **Code**:
```java
package com.sareekart.entity;

public enum ReturnType {
    RETURN,
    EXCHANGE
}
```
- **Semantics**:
  - `RETURN`: Customer requests monetary return / store credit.
  - `EXCHANGE`: Customer requests a replacement saree (`exchangeSku` is populated).

---

### 2.3 `ReturnReason.java`
- **File Path**: `backend/backend/src/main/java/com/sareekart/entity/ReturnReason.java`
- **Package**: `com.sareekart.entity`
- **Code**:
```java
package com.sareekart.entity;

public enum ReturnReason {
    COLOR_MISMATCH,
    ZARI_DEFECT,
    FABRIC_FEEL,
    INCORRECT_ITEM,
    SIZE_MISMATCH,
    OTHER
}
```
- **Taxonomy Descriptions**:
  - `COLOR_MISMATCH`: Real garment color differs noticeably from catalog photography.
  - `ZARI_DEFECT`: Metallic zari thread fraying, loose weaving, or tarnished brocade.
  - `FABRIC_FEEL`: Texture or weight non-compliant with pure silk/handloom expectations.
  - `INCORRECT_ITEM`: Wrong SKU or design variant delivered.
  - `SIZE_MISMATCH`: Saree length, border width, or unstitched blouse piece dimensions incorrect.
  - `OTHER`: Any other customer-specified reason provided in `comments`.

---

### 2.4 `RefundMode.java`
- **File Path**: `backend/backend/src/main/java/com/sareekart/entity/RefundMode.java`
- **Package**: `com.sareekart.entity`
- **Code**:
```java
package com.sareekart.entity;

public enum RefundMode {
    ORIGINAL_PAYMENT,
    STORE_CREDIT,
    EXCHANGE_DRAPE
}
```
- **Semantics**:
  - `ORIGINAL_PAYMENT`: Refund credited back to original payment method (Card, UPI, NetBanking via Razorpay, or bank transfer for COD).
  - `STORE_CREDIT`: Refund credited to customer wallet / store credits.
  - `EXCHANGE_DRAPE`: Value applied directly toward replacement saree exchange item.

---

## 3. JPA Entity Specification: `ReturnRequest.java`

- **File Path**: `backend/backend/src/main/java/com/sareekart/entity/ReturnRequest.java`
- **Package**: `com.sareekart.entity`
- **Table Name**: `return_requests`

### 3.1 Field-by-Field Mapping Matrix

| Field Name | Java Type | JPA & Hibernate Annotations | Column Name | Constraints / Details |
|---|---|---|---|---|
| `id` | `Long` | `@Id @GeneratedValue(strategy = GenerationType.IDENTITY)` | `id` | PK, Auto Increment |
| `order` | `Order` | `@ManyToOne(fetch = FetchType.LAZY)`<br>`@JoinColumn(name = "order_id", nullable = false, unique = true)` | `order_id` | Foreign Key to `orders(id)`, Unique Constraint |
| `user` | `User` | `@ManyToOne(fetch = FetchType.LAZY)`<br>`@JoinColumn(name = "user_id", nullable = false)` | `user_id` | Foreign Key to `users(id)` |
| `type` | `ReturnType` | `@Enumerated(EnumType.STRING)`<br>`@Column(nullable = false, length = 30)` | `type` | Default `ReturnType.RETURN` |
| `reason` | `ReturnReason` | `@Enumerated(EnumType.STRING)`<br>`@Column(nullable = false, length = 50)` | `reason` | Required enum |
| `comments` | `String` | `@Column(columnDefinition = "TEXT")` | `comments` | Detailed customer explanation |
| `status` | `ReturnStatus` | `@Enumerated(EnumType.STRING)`<br>`@Column(nullable = false, length = 30)` | `status` | Default `ReturnStatus.PENDING` |
| `images` | `List<String>` | `@Convert(converter = StringListConverter.class)`<br>`@Column(name = "images", columnDefinition = "TEXT")` | `images` | JSON array string of up to 3 photo URLs |
| `refundAmount` | `BigDecimal` | `@Column(name = "refund_amount", precision = 10, scale = 2)` | `refund_amount` | Matches order total by default |
| `refundMode` | `RefundMode` | `@Enumerated(EnumType.STRING)`<br>`@Column(name = "refund_mode", length = 50)` | `refund_mode` | Enum representation |
| `exchangeSku` | `String` | `@Column(name = "exchange_sku", length = 100)` | `exchange_sku` | Required if `type == EXCHANGE` |
| `reverseCourier` | `String` | `@Column(name = "reverse_courier", length = 100)` | `reverse_courier` | Assigned on `PICKUP_SCHEDULED` |
| `reverseTrackingNumber` | `String` | `@Column(name = "reverse_tracking_number", length = 100)` | `reverse_tracking_number` | Reverse AWB assigned on pickup |
| `adminNotes` | `String` | `@Column(name = "admin_notes", columnDefinition = "TEXT")` | `admin_notes` | Mandatory when `status == REJECTED` |
| `createdAt` | `LocalDateTime` | `@CreatedDate @Column(name = "created_at", updatable = false)` | `created_at` | JPA Auditing timestamp |
| `updatedAt` | `LocalDateTime` | `@LastModifiedDate @Column(name = "updated_at")` | `updated_at` | JPA Auditing timestamp |

---

### 3.2 Complete Code Implementation Specification

```java
package com.sareekart.entity;

import com.sareekart.converter.StringListConverter;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "return_requests",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_return_requests_order", columnNames = {"order_id"})
    },
    indexes = {
        @Index(name = "idx_return_requests_user", columnList = "user_id"),
        @Index(name = "idx_return_requests_order", columnList = "order_id"),
        @Index(name = "idx_return_requests_status", columnList = "status"),
        @Index(name = "idx_return_requests_created_at", columnList = "created_at")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReturnRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private ReturnType type = ReturnType.RETURN;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ReturnReason reason;

    @Column(columnDefinition = "TEXT")
    private String comments;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private ReturnStatus status = ReturnStatus.PENDING;

    @Convert(converter = StringListConverter.class)
    @Column(name = "images", columnDefinition = "TEXT")
    @Builder.Default
    private List<String> images = new ArrayList<>();

    @Column(name = "refund_amount", precision = 10, scale = 2)
    private BigDecimal refundAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "refund_mode", length = 50)
    private RefundMode refundMode;

    @Column(name = "exchange_sku", length = 100)
    private String exchangeSku;

    @Column(name = "reverse_courier", length = 100)
    private String reverseCourier;

    @Column(name = "reverse_tracking_number", length = 100)
    private String reverseTrackingNumber;

    @Column(name = "admin_notes", columnDefinition = "TEXT")
    private String adminNotes;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ================= Convenience Accessors =================
    public Long getOrderId() {
        return order != null ? order.getId() : null;
    }

    public Long getUserId() {
        return user != null ? user.getId() : null;
    }

    // ================= Defensive Lifecycle Callbacks =================
    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.updatedAt == null) {
            this.updatedAt = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = ReturnStatus.PENDING;
        }
        if (this.type == null) {
            this.type = ReturnType.RETURN;
        }
        if (this.images == null) {
            this.images = new ArrayList<>();
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
```

---

### 3.3 Images Persistence Architecture: `StringListConverter.java`

To satisfy the requirement that `images` in Java is a `List<String>` of up to 3 photo URLs while storing the serialized URLs cleanly in the `images TEXT` column of `return_requests` (matching `V17__create_return_requests_table.sql` and avoiding extraneous child tables), use a JPA `AttributeConverter`.

- **File Path**: `backend/backend/src/main/java/com/sareekart/converter/StringListConverter.java`
- **Package**: `com.sareekart.converter`
- **Code**:
```java
package com.sareekart.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Converter(autoApply = false)
public class StringListConverter implements AttributeConverter<List<String>, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<String> list) {
        if (list == null || list.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            return String.join(",", list);
        }
    }

    @Override
    public List<String> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty() || dbData.trim().equals("[]")) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(dbData, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            // Fallback for comma-separated values
            return Arrays.stream(dbData.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        }
    }
}
```

#### Alternative: `@ElementCollection`
If the team opts for an `@ElementCollection` instead of `AttributeConverter`, the annotation on `ReturnRequest` is:
```java
@ElementCollection(fetch = FetchType.EAGER)
@CollectionTable(name = "return_request_images", joinColumns = @JoinColumn(name = "return_request_id"))
@Column(name = "image_url", length = 1000)
@OrderColumn(name = "image_order")
@Builder.Default
private List<String> images = new ArrayList<>();
```
*Note*: `StringListConverter` is recommended because `V17` defines `images TEXT NULL` directly on `return_requests`, requiring only one table.

---

## 4. Spring Data JPA Repository Specification: `ReturnRequestRepository.java`

- **File Path**: `backend/backend/src/main/java/com/sareekart/repository/ReturnRequestRepository.java`
- **Package**: `com.sareekart.repository`

### 4.1 Interface Specification

```java
package com.sareekart.repository;

import com.sareekart.entity.ReturnRequest;
import com.sareekart.entity.ReturnStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, Long> {

    /**
     * Finds all return requests submitted by a specific user, newest first.
     * Navigates returnRequest.user.id == userId.
     */
    List<ReturnRequest> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Finds the return request associated with a specific order.
     * Guaranteed at most one result due to unique constraint on order_id.
     */
    Optional<ReturnRequest> findByOrderId(Long orderId);

    /**
     * Finds all return requests matching a specific status enum, newest first.
     */
    List<ReturnRequest> findByStatusOrderByCreatedAtDesc(ReturnStatus status);

    /**
     * Finds all return requests across all statuses, newest first.
     */
    List<ReturnRequest> findAllByOrderByCreatedAtDesc();

    /**
     * Checks whether a return request already exists for an order.
     * Used for duplicate prevention prior to claim creation.
     */
    boolean existsByOrderId(Long orderId);

    /**
     * Counts returns by status for Admin KPI cards (Pending Review, Pickups Scheduled, Completed, etc.).
     */
    long countByStatus(ReturnStatus status);

    /**
     * Overloaded helper query to support string-based status filtering from REST endpoints.
     */
    @Query("SELECT r FROM ReturnRequest r WHERE str(r.status) = :status ORDER BY r.createdAt DESC")
    List<ReturnRequest> findByStatusStringOrderByCreatedAtDesc(@Param("status") String status);
}
```

### 4.2 Query Execution & Mechanics Analysis

1. **`findByUserIdOrderByCreatedAtDesc(Long userId)`**:
   - Evaluated by Spring Data JPA query derivation on the `User user` association: `r.user.id = :userId`.
   - SQL: `SELECT r.* FROM return_requests r WHERE r.user_id = ? ORDER BY r.created_at DESC`.
   - Backed by `idx_return_requests_user` and `idx_return_requests_created_at`.
2. **`findByOrderId(Long orderId)`**:
   - Evaluated on the `Order order` association: `r.order.id = :orderId`.
   - SQL: `SELECT r.* FROM return_requests r WHERE r.order_id = ?`.
   - Backed by `uk_return_requests_order` unique index.
3. **`findByStatusOrderByCreatedAtDesc(ReturnStatus status)`**:
   - Evaluated on the `ReturnStatus status` enum property.
   - Spring Data JPA automatically binds `status.name()` to the query parameter.
   - SQL: `SELECT r.* FROM return_requests r WHERE r.status = ? ORDER BY r.created_at DESC`.
   - Backed by `idx_return_requests_status` index.
4. **`findAllByOrderByCreatedAtDesc()`**:
   - Retrieves all rows ordered by creation timestamp descending.
   - SQL: `SELECT r.* FROM return_requests r ORDER BY r.created_at DESC`.
5. **`existsByOrderId(Long orderId)`**:
   - Emits a high-performance `SELECT CASE WHEN COUNT(r.id) > 0 THEN TRUE ELSE FALSE END` query.

---

## 5. Flyway Schema Migration Specification: `V17__create_return_requests_table.sql`

- **File Path**: `backend/backend/src/main/resources/db/migration/V17__create_return_requests_table.sql`
- **Target Folder**: `backend/backend/src/main/resources/db/migration/` (Downstream Worker will create this directory).

### 5.1 SQL DDL Script

```sql
-- ==============================================================================
-- Migration V17: Create return_requests Table for Self-Service Returns & Exchanges
-- SareeKart v3.0 Module 1: Customer Returns and Exchanges
-- ==============================================================================

CREATE TABLE IF NOT EXISTS return_requests (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    type VARCHAR(30) NOT NULL DEFAULT 'RETURN',
    reason VARCHAR(50) NOT NULL,
    comments TEXT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    images TEXT NULL,
    refund_amount DECIMAL(10, 2) NULL,
    refund_mode VARCHAR(50) NULL,
    exchange_sku VARCHAR(100) NULL,
    reverse_courier VARCHAR(100) NULL,
    reverse_tracking_number VARCHAR(100) NULL,
    admin_notes TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_return_requests_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE RESTRICT,
    CONSTRAINT fk_return_requests_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT uk_return_requests_order UNIQUE (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Query optimization indexes
CREATE INDEX idx_return_requests_user ON return_requests (user_id);
CREATE INDEX idx_return_requests_order ON return_requests (order_id);
CREATE INDEX idx_return_requests_status ON return_requests (status);
CREATE INDEX idx_return_requests_created_at ON return_requests (created_at);
```

### 5.2 Database Compatibility & Idempotency Notes
- **MySQL 8.x**:
  - `ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci` ensures proper Unicode support for customer comments and emojis.
  - `ON UPDATE CURRENT_TIMESTAMP` handles automatic timestamp updates at the database level.
- **H2 In-Memory (Test Suite)**:
  - Configured with `MODE=MySQL`. H2 2.x natively accepts `AUTO_INCREMENT`, `VARCHAR`, `TIMESTAMP`, `ON UPDATE CURRENT_TIMESTAMP`, `FOREIGN KEY`, and `UNIQUE` constraints.
  - In unit test runs (`src/test/resources/application-test.yaml`), Hibernate's `ddl-auto: create-drop` reads JPA annotations to create schema tables directly.

---

## 6. Order Domain Enhancements (Delivery Timestamp)

### 6.1 Context & Findings
- `Order.java` currently has `createdAt` and `updatedAt`, but lacks a dedicated `deliveredAt` column.
- When an order transitions to `DELIVERED`, `OrderServiceImpl.updateOrderStatus` modifies `status` and `updatedAt`.

### 6.2 Implementation Recommendations for Worker
1. **Enhance `Order.java`**:
   Add the following property:
   ```java
   @Column(name = "delivered_at")
   private LocalDateTime deliveredAt;
   ```
2. **Update `OrderServiceImpl.java`**:
   When status transitions to `DELIVERED`:
   ```java
   if (orderStatus == OrderStatus.DELIVERED && order.getDeliveredAt() == null) {
       order.setDeliveredAt(LocalDateTime.now());
   }
   ```
3. **Defensive Delivery Calculation in `ReturnServiceImpl`**:
   To guarantee 100% backwards compatibility with seeded or legacy orders where `deliveredAt` is null:
   ```java
   LocalDateTime deliveryTime = order.getDeliveredAt() != null
           ? order.getDeliveredAt()
           : (order.getUpdatedAt() != null ? order.getUpdatedAt() : order.getCreatedAt());
   ```

---

## 7. Downstream Worker Implementation Checklist & Verification

### 7.1 File Generation Order
1. `backend/backend/src/main/java/com/sareekart/entity/ReturnStatus.java`
2. `backend/backend/src/main/java/com/sareekart/entity/ReturnType.java`
3. `backend/backend/src/main/java/com/sareekart/entity/ReturnReason.java`
4. `backend/backend/src/main/java/com/sareekart/entity/RefundMode.java`
5. `backend/backend/src/main/java/com/sareekart/converter/StringListConverter.java`
6. `backend/backend/src/main/java/com/sareekart/entity/ReturnRequest.java`
7. `backend/backend/src/main/java/com/sareekart/repository/ReturnRequestRepository.java`
8. `backend/backend/src/main/resources/db/migration/V17__create_return_requests_table.sql`
9. `Order.java` enhancement (add `deliveredAt` field and getter/setter).

### 7.2 Independent Verification Commands
```bash
# 1. Verify storage headroom:
/Users/chaitanyachaitu/scripts/check_disk_health.sh

# 2. Compile backend domain entities and repository:
cd /Users/chaitanyachaitu/Downloads/SareeKart-main/backend/backend
./mvnw compile-test

# 3. Execute backend unit test suite:
./mvnw test -Dtest=ReturnServiceImplTest

# 4. Execute full backend regression:
./mvnw test
```
