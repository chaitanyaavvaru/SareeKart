# SareeKart — Phase 11 Discovery Report

> **Document Status:** DISCOVERY ONLY — AUDIT, GAP ANALYSIS & CANDIDATE FORMULATION  
> **Rule Enforcement:** Zero code changes, zero database mutations, zero configuration edits, zero commits.  
> **Preceding Phases (Frozen & Preserved):**  
> - Phase 1 (Product + Image Lifecycle): ✅ Complete & Frozen  
> - Phase 2 (Categories + Product Attributes): ✅ Complete & Frozen  
> - Phase 3 (Search + Filtering): ✅ Complete & Frozen  
> - Phase 4 (Cart + Wishlist): ✅ Complete & Frozen  
> - Phase 5 (Orders + Inventory): ✅ Complete & Frozen  
> - Phase 6 (Customer Behavior + Telemetry): ✅ Complete & Frozen  
> - Phase 7 (Neo4j Knowledge Graph): ✅ Complete, Verified & Frozen  
> - Phase 8 (AI Recommendations & Hybrid Ranking): ✅ Complete, Verified & Frozen  
> - Phase 9 (AI Luxury Saree Stylist & Drape Concierge): ✅ Complete, Verified & Frozen  
> - Phase 10 (WhatsApp AI Commerce Assistant): ✅ Complete, Verified & Frozen  
> **Current Target:** Phase 11 — Collaborative Bridal Trousseau Studio & Multi-Party Wedding Wardrobe Curator

---

## A. Current System Audit

SareeKart is an enterprise-grade luxury e-commerce platform dedicated to authentic Indian handloom sarees (Kanchipuram, Banarasi, Chanderi, Paithani, Patola, Tussar, Baluchari). Phases 1 through 10 have established an omnichannel luxury shopping engine.

### 1. Backend Architecture
- **Runtime & Framework:** Java 17 LTS, Spring Boot 3.5.15, Spring Data JPA, Spring Security, Spring WebSocket (STOMP).
- **Application Server:** Runs on port 8081.
- **Service Layer Pattern:** Clean Domain-Driven Service pattern (`ProductService`, `CategoryService`, `CartService`, `OrderService`, `CustomerEventService`, `Neo4jGraphService`, `RecommendationService`, `AiStylistService`, `WhatsAppWebhookService`, `WhatsAppApiClient`).
- **Data Persistence:** Spring Data JPA over Hibernate with strict validation and repository abstraction.

### 2. Frontend Architecture
- **Framework & Tooling:** React 18, Vite, React Router DOM 6.
- **Styling:** TailwindCSS with custom Indian luxury heritage palette (`#4a0e17` deep maroon, `#b8860b` antique gold, `#fffaf0` ivory silk).
- **State Management:** React Context (`AuthContext`, `CartContext`, `WishlistContext`).
- **Bundle Discipline:** All chunks strictly `< 500 kB` (largest vendor chunk is 227 kB). Pure SVG and CSS for data visualizations; zero heavy external charting libraries.
- **Dev Server:** Port 5173 with automatic `/api` proxying to `http://localhost:8081`.

### 3. MySQL Schema (Historical Migrations V17–V28)
- **Engine:** MySQL 8.0 running on port 3307 (`sareekart_db`).
- **`V17`–`V20`**: Base catalog (`products`, `product_images`), user accounts (`users`, `roles`), inventory items.
- **`V21`**: WhatsApp notification logs (`whatsapp_notification_logs`) and user opt-in flags.
- **`V22`**: Customer telemetry events (`customer_events`).
- **`V23`**: Graph sync tracking & change-log mirrors.
- **`V24`**: Normalized taxonomy (`categories`, `fabrics`, `occasions`, `colors`, foreign key normalization).
- **`V25`**: Cart & Wishlist persistence with multi-item deduplication and tenant isolation.
- **`V26`**: Order lifecycle, shipments, AWB courier tracking, reverse pickup states.
- **`V27`**: Session-level customer telemetry event stream.
- **`V28`**: WhatsApp bidirectional conversations (`whatsapp_contacts`, `conversations`, `whatsapp_messages`, `idempotency_keys`).

### 4. Neo4j Topology
- **Engine:** Neo4j 5.x on bolt://localhost:7687 (`neo4j`/`sareekart2026`).
- **Nodes:** `(:Saree)`, `(:Category)`, `(:Fabric)`, `(:Occasion)`, `(:Color)`, `(:WeaveCluster)`, `(:User)`.
- **Relationships:** `[:BELONGS_TO]`, `[:MADE_OF]`, `[:SUITABLE_FOR]`, `[:HAS_COLOR]`, `[:ORIGINATED_FROM]`, `[:VIEWED]`, `[:PURCHASED]`, `[:PAIRED_WITH]`.
- **Resilience:** Thread-pool bounded, Circuit Breaker protected (`GraphCircuitBreaker`), with automatic deterministic SQL fallbacks if the graph instance is unreachable.

### 5. Existing AI & Stylist Services
- **`SareeEmbeddingService`**: Computes 384-dimensional dense semantic vectors using lightweight models.
- **`RecommendationService`**: 5-factor hybrid scoring ranker (Content similarity, Graph traversal co-purchases, Telemetry recency, Occasion affinity, Price elasticity).
- **`AiStylistService`**: Spring AI / Gemini 2.5 Flash integration with structured JSON prompt grounding. Suggests 3 complete ensembles (Saree + Jewelry + Blouse cut + Footwear) strictly from catalog inventory.
- **`GroundingService`**: Verifies every recommended SKU exists, is active, and has available inventory > 0 before presenting to users.

### 6. WhatsApp Architecture (Phase 10)
- **Meta WhatsApp Cloud API v19.0**: Inbound webhooks validated with HMAC-SHA256 (`X-Hub-Signature-256`).
- **Idempotency**: Strict deduplication using `wam_id` in database table `whatsapp_messages` and temporary Redis/in-memory locks.
- **Clienteling Inbox**: Real-time admin conversation view (`/admin/whatsapp`) with WebSocket STOMP messaging for human boutique escalation.
- **Interactive Messaging**: Standard templates, interactive reply buttons (`DISCOVER_MORE`, `ADD_TO_CART`, `TALK_TO_STYLIST`), and outbound dispatch client.

### 7. Telemetry & Event Architecture
- **`CustomerEventService`**: Asynchronous ring-buffered event ingestion for customer actions (`VIEW_ITEM`, `SEARCH`, `ADD_TO_CART`, `WISHLIST`, `STYLIST_QUERY`).
- **Privacy Controls**: Anonymized session cookies; zero customer PII stored in event payloads.

### 8. Authentication & Security
- **JWT Authentication**: Stateless token-based auth with HTTP-only cookies and Authorization headers.
- **Role Hierarchy**: `CUSTOMER`, `STAFF`, `MANAGER`, `ADMIN`, `OWNER`.
- **Method Security**: `@PreAuthorize("hasRole('...')")` on privileged admin operations; tenant isolation on customer resources (`cart`, `wishlist`, `orders`).

### 9. Test Architecture
- **Backend**: JUnit 5, Mockito, Spring Boot Test. **362 / 362 passing tests (100%)**.
- **Frontend**: Vitest, React Testing Library.
- **E2E Integration**: Playwright test suite (Chromium headless) validating full UI workflows.

---

## B. Gap Analysis

Despite the breadth of Phases 1–10, systematic inspection reveals critical product and architectural gaps at the intersection of luxury bridal commerce, multi-party decision making, and storefront capabilities:

### 1. The Bridal Trousseau Storefront Debt
- **Inspection Finding:** File `frontend/src/pages/Bridal/TrousseauPlannerPage.jsx` contains 469 lines of custom-crafted bridal ceremony UI.
- **Defect:** It operates entirely on hardcoded static JSON arrays (`SAMPLE_CEREMONIES`) with synthetic IDs (`eng-1`, `hal-1`). Users cannot save changes, cannot link authentic catalog sarees to ceremonies, and lose all selections upon page refresh.
- **Revenue Impact:** Bridal trousseau shopping represents $> 65\%$ of luxury handloom GMV (average order value ₹1.5L to ₹5L across 3–7 ceremonies: Engagement, Haldi, Mehendi, Sangeet, Muhurtham, Reception).

### 2. Single-Tenant Cart vs. Collective Indian Wedding Buying
- **Inspection Finding:** Saree purchases in India, especially bridal and high-end wedding handlooms, are rarely individual decisions. They involve the bride, groom, mother, mother-in-law, sisters, and trusted family advisors.
- **Defect:** SareeKart's Cart (`CartService`) and Wishlist (`WishlistService`) are strictly single-user, authenticated by a single `userId`.
- **Current Friction:** Customers resort to taking screenshots and sharing unorganized images over WhatsApp, resulting in cart abandonment, confusion regarding which saree was chosen for which ceremony, and loss of attribution.

### 3. Disconnect Between AI Stylist Ensembles and Structured Purchasing
- **Inspection Finding:** Phase 9 (`AiStylistService`) successfully suggests multi-ceremony wardrobe ensembles based on wedding color palettes and themes.
- **Defect:** There is no bridge to persist these multi-saree ensembles into a collaborative shopping plan where family members can vote, comment, and approve each look before checkout.

### 4. Technical & Operational Debt
- **Voting & Feedback Void:** There is currently no mechanism for non-authenticated or guest family members to cast lightweight votes ("Love", "Like", "Pass") on shortlisted sarees.
- **Lack of Bulk Ceremony Checkout:** Converting a 5-ceremony trousseau into orders requires manually searching and adding each saree to the cart one by one, introducing friction at the highest-margin point of the funnel.

---

## C. Phase 11 Candidate Capability: Collaborative Bridal Trousseau Studio & Multi-Party Wedding Wardrobe Curator

The recommended Phase 11 subsystem is:
**The Collaborative Bridal Trousseau Studio & Multi-Party Wedding Wardrobe Curator**.

### Factual Architectural Relationship to Phases 1–10:
1. **Relationship to Phase 1–3 (Catalog & Search):**
   - The Trousseau Studio queries `ProductRepository` and `ProductSpecificationBuilder` to allow brides and family curators to search and assign authentic handloom sarees directly to specific wedding ceremonies (e.g. Kanchipuram Brocade for Muhurtham, Pastel Organza for Mehendi).
2. **Relationship to Phase 4 (Cart & Wishlist):**
   - The Studio does not alter `Cart` or `Wishlist` schemas. Instead, it provides an external, idempotent "Transfer Ceremony to Cart" bridge. When the family finalizes selections, a single click converts approved trousseau items into `CartItem`s via the existing, frozen `CartService.addToCart()` API.
3. **Relationship to Phase 5 (Orders & Inventory):**
   - The Studio reads stock availability from `InventoryService` in real time. If a shortlisted bridal saree has low stock (`stock <= 2`), the studio highlights "Limited Weave Alert" to expedite family consensus.
4. **Relationship to Phase 6 (Customer Behavior & Telemetry):**
   - Emits structured telemetry events (`TROUSSEAU_BOARD_CREATED`, `CEREMONY_ITEM_ADDED`, `COLLABORATOR_INVITED`, `FAMILY_VOTE_CAST`, `TROUSSEAU_CHECKOUT_INITIATED`) via the existing `CustomerEventService`.
5. **Relationship to Phase 7 (Neo4j Knowledge Graph):**
   - Leverages existing `[:SUITABLE_FOR]` relationships between `(:Saree)` and `(:Occasion)` nodes (`WEDDING`, `BRIDAL`, `RECEPTION`, `SANGEET`, `HALDI`) to recommend ceremony-appropriate options.
6. **Relationship to Phase 8 (AI Recommendations & Hybrid Ranking):**
   - Uses `RecommendationService` to automatically generate "Family Complementary Suggestions" (e.g. recommending mother-of-the-bride and bridesmaid sarees that complement the bride's selected Muhurtham color palette).
7. **Relationship to Phase 9 (AI Luxury Saree Stylist):**
   - Bridges the AI Stylist output directly into the Trousseau Studio: brides can click "Save Ensemble to Trousseau Board" directly from the AI Stylist chat drawer.
8. **Relationship to Phase 10 (WhatsApp AI Commerce Assistant):**
   - Integrates with `WhatsAppApiClient` to allow brides to share interactive WhatsApp voting cards with family members. Family members can vote ("Love it 👍", "Needs more zari ✨", "Change color 🎨") directly via WhatsApp reply buttons or instant deep links.

---

## D. Frozen-Phase Protection Matrix

The following table explicitly documents that Phase 11 is strictly additive and leaves Phases 1–10 untouchable:

| Phase | Module / Artifacts | Status | Phase 11 Interaction Boundary |
|---|---|---|---|
| **Phase 1** | `Product`, `ProductImage`, `ProductController` | FROZEN | Read-only access via `ProductRepository`. No column additions or logic edits. |
| **Phase 2** | `Category`, `Fabric`, `Occasion`, `Color`, `V24` | FROZEN | Read-only lookup via `AttributeLookupService`. Taxonomy unchanged. |
| **Phase 3** | `ProductSpecificationBuilder`, Filter endpoints | FROZEN | Invoked as a standard client for catalog searches. |
| **Phase 4** | `Cart`, `CartItem`, `Wishlist`, `CartService`, `V25` | FROZEN | Target of conversion: approved items transferred using existing `CartService.addItem()` method. Zero schema modifications. |
| **Phase 5** | `Order`, `OrderItem`, `InventoryItem`, `OrderService`, `V26` | FROZEN | Standard order placement remains unchanged. Stock read via `Product.stockQuantity`. |
| **Phase 6** | `CustomerEvent`, `CustomerEventService`, `V27` | FROZEN | New event types logged to existing `customer_events` table without modifying schema. |
| **Phase 7** | `Neo4jGraphService`, Graph projections | FROZEN | Read-only Cypher queries for occasion-based sarees; circuit breaker preserved. |
| **Phase 8** | `RecommendationService`, `SareeEmbeddingService` | FROZEN | Invoked to rank ceremony recommendations; hybrid weighting math untouched. |
| **Phase 9** | `AiStylistService`, `GroundingService` | FROZEN | AI Stylist prompts and validation pipelines untouched; client-side links added to route output into trousseau boards. |
| **Phase 10** | `WhatsAppWebhookService`, `WhatsAppApiClient`, `V28` | FROZEN | Outbound message helper sends trousseau share notifications. Existing webhook ingestion untouched. |
