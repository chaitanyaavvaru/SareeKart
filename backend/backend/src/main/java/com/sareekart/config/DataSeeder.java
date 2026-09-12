package com.sareekart.config;

import com.sareekart.entity.Category;
import com.sareekart.entity.Product;
import com.sareekart.entity.User;
import com.sareekart.entity.Coupon;
import com.sareekart.repository.CategoryRepository;
import com.sareekart.repository.ProductRepository;
import com.sareekart.repository.UserRepository;
import com.sareekart.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CouponRepository couponRepository;
    private final com.sareekart.repository.InventoryItemRepository inventoryItemRepository;
    private final com.sareekart.repository.OrderRepository orderRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void run(String... args) {
        if (userRepository.count() == 0) {
            log.info("Seeding users...");
            seedUsers();
        }
        ensureOwnerAndManager();
        if (categoryRepository.count() == 0) {
            log.info("Seeding categories and products...");
            seedCategories();
            seedProducts();
        }
        if (inventoryItemRepository.count() == 0) {
            log.info("Seeding inventory items...");
            seedInventory();
        }
        if (couponRepository.count() == 0) {
            log.info("Seeding promo coupons...");
            seedCoupons();
        }
        if (orderRepository.count() == 0) {
            log.info("Seeding historical order telemetry for analytics...");
            seedHistoricalOrders();
            log.info("Historical orders seeded successfully!");
        }
        log.info("Data seeding completed successfully!");
    }

    private void ensureOwnerAndManager() {
        if (!userRepository.existsByEmail("owner@sareekart.com")) {
            User owner = User.builder()
                    .firstName("Super")
                    .lastName("Owner")
                    .email("owner@sareekart.com")
                    .mobile("9876543212")
                    .password(passwordEncoder.encode("owner123"))
                    .role(com.sareekart.entity.Role.OWNER)
                    .build();
            userRepository.save(owner);
            log.info("Owner account owner@sareekart.com seeded successfully!");
        }
        if (!userRepository.existsByEmail("manager@sareekart.com")) {
            User manager = User.builder()
                    .firstName("Store")
                    .lastName("Manager")
                    .email("manager@sareekart.com")
                    .mobile("9876543213")
                    .password(passwordEncoder.encode("manager123"))
                    .role(com.sareekart.entity.Role.MANAGER)
                    .build();
            userRepository.save(manager);
            log.info("Manager account manager@sareekart.com seeded successfully!");
        }
    }

    @org.springframework.transaction.annotation.Transactional
    private void seedInventory() {
        List<Product> products = productRepository.findAll();
        List<com.sareekart.entity.InventoryItem> items = new java.util.ArrayList<>();
        for (Product p : products) {
            String sku = "SK-" + p.getName().toUpperCase().replaceAll("[^A-Z0-9]+", "-");
            if (sku.length() > 25) sku = sku.substring(0, 25);
            sku = sku + "-" + p.getId();

            String catName = p.getFabric() != null ? p.getFabric() : "Handloom Silk";
            try {
                if (p.getCategory() != null) catName = p.getCategory().getName();
            } catch (Exception ignored) {}

            com.sareekart.entity.InventoryItem item = com.sareekart.entity.InventoryItem.builder()
                    .sku(sku)
                    .productId(p.getId())
                    .productName(p.getName())
                    .category(catName)
                    .warehouseCode("WH-01")
                    .warehouseName("Bengaluru Central Fulfillment Hub")
                    .binLocation("A" + (p.getId() % 5 + 1) + "-R" + (p.getId() % 3 + 1) + "-S1")
                    .onHand(p.getStockQuantity() != null ? p.getStockQuantity() : 10)
                    .reserved(Math.min(2, p.getStockQuantity() != null ? p.getStockQuantity() / 3 : 0))
                    .unitPrice(p.getPrice())
                    .build();
            item.recalculateStatus();
            items.add(item);
        }
        if (!items.isEmpty()) {
            inventoryItemRepository.saveAll(items);
            log.info("Seeded {} persistent inventory items", items.size());
        }
    }

    private void seedUsers() {
        User admin = User.builder()
                .firstName("SareeKart")
                .lastName("Admin")
                .email("admin@sareekart.com")
                .mobile("9876543210")
                .password(passwordEncoder.encode("admin123"))
                .role(com.sareekart.entity.Role.ADMIN)
                .build();

        User customer = User.builder()
                .firstName("Chaitanya")
                .lastName("Customer")
                .email("customer@sareekart.com")
                .mobile("9876543211")
                .password(passwordEncoder.encode("customer123"))
                .role(com.sareekart.entity.Role.CUSTOMER)
                .build();

        userRepository.saveAll(List.of(admin, customer));
        log.info("Admin and customer users seeded successfully!");
    }

    private void seedCategories() {
        List<Category> categories = List.of(
            Category.builder().name("Silk Sarees").description("Premium silk sarees from renowned weavers").build(),
            Category.builder().name("Cotton Sarees").description("Comfortable and elegant cotton sarees").build(),
            Category.builder().name("Chiffon Sarees").description("Lightweight and flowing chiffon sarees").build(),
            Category.builder().name("Georgette Sarees").description("Graceful georgette sarees for every occasion").build(),
            Category.builder().name("Banarasi Sarees").description("Exquisite Banarasi sarees with intricate zari work").build(),
            Category.builder().name("Kanchipuram Sarees").description("Traditional Kanchipuram silk sarees").build(),
            Category.builder().name("Designer Sarees").description("Trendy designer sarees for modern women").build(),
            Category.builder().name("Bridal Sarees").description("Magnificent bridal sarees for your special day").build()
        );
        categoryRepository.saveAll(categories);
    }

    private void seedProducts() {
        Category silk = categoryRepository.findByName("Silk Sarees").orElseThrow();
        Category cotton = categoryRepository.findByName("Cotton Sarees").orElseThrow();
        Category banarasi = categoryRepository.findByName("Banarasi Sarees").orElseThrow();
        Category kanchipuram = categoryRepository.findByName("Kanchipuram Sarees").orElseThrow();
        Category chiffon = categoryRepository.findByName("Chiffon Sarees").orElseThrow();
        Category georgette = categoryRepository.findByName("Georgette Sarees").orElseThrow();
        Category designer = categoryRepository.findByName("Designer Sarees").orElseThrow();
        Category bridal = categoryRepository.findByName("Bridal Sarees").orElseThrow();

        List<Product> products = List.of(
            Product.builder()
                .name("Royal Banarasi Silk Saree")
                .description("Handwoven Banarasi silk saree with intricate gold zari work. Perfect for weddings and grand celebrations.")
                .price(new BigDecimal("18999.00"))
                .category(banarasi)
                .stockQuantity(25)
                .fabric("Silk")
                .occasion("Wedding")
                .color("Ruby Red")
                .images(List.of("https://images.unsplash.com/photo-1610030469983-98e550d6193c?auto=format&fit=crop&fm=webp&w=800&q=80"))
                .build(),
            Product.builder()
                .name("Kanchipuram Temple Border Saree")
                .description("Authentic Kanchipuram silk saree with traditional temple border design and rich pallu.")
                .price(new BigDecimal("26133.00"))
                .category(kanchipuram)
                .stockQuantity(15)
                .fabric("Silk")
                .occasion("Bridal")
                .color("Gold")
                .images(List.of("https://images.unsplash.com/photo-1621184455862-c163dfb30e0f?auto=format&fit=crop&fm=webp&w=800&q=80"))
                .build(),
            Product.builder()
                .name("Elegant Cotton Handloom Saree")
                .description("Soft handloom cotton saree with beautiful jamdani weave. Ideal for daily wear and office.")
                .price(new BigDecimal("8667.00"))
                .category(cotton)
                .stockQuantity(50)
                .fabric("Cotton")
                .occasion("Casual")
                .color("White")
                .images(List.of("https://images.unsplash.com/photo-1594736797933-d0501ba2fe65?auto=format&fit=crop&fm=webp&w=800&q=80"))
                .build(),
            Product.builder()
                .name("Designer Chiffon Party Saree")
                .description("Stunning chiffon saree with sequin work and designer blouse piece. Perfect for parties.")
                .price(new BigDecimal("14500.00"))
                .category(chiffon)
                .stockQuantity(30)
                .fabric("Chiffon")
                .occasion("Party")
                .color("Pink")
                .images(List.of("https://images.unsplash.com/photo-1609357605129-26f69add5d6e?auto=format&fit=crop&fm=webp&w=800&q=80"))
                .build(),
            Product.builder()
                .name("Pure Georgette Embroidered Saree")
                .description("Luxurious georgette saree with heavy embroidery work and stone detailing.")
                .price(new BigDecimal("24111.00"))
                .category(georgette)
                .stockQuantity(20)
                .fabric("Georgette")
                .occasion("Festival")
                .color("Green")
                .images(List.of("https://images.unsplash.com/photo-1617627143750-d86bc21e42bb?auto=format&fit=crop&fm=webp&w=800&q=80"))
                .build(),
            Product.builder()
                .name("Tussar Silk Printed Saree")
                .description("Natural tussar silk saree with hand block prints. Eco-friendly and stylish.")
                .price(new BigDecimal("9800.00"))
                .category(silk)
                .stockQuantity(35)
                .fabric("Tussar Silk")
                .occasion("Casual")
                .color("Beige")
                .images(List.of("https://images.unsplash.com/photo-1566552881560-0be862a7c445?auto=format&fit=crop&fm=webp&w=800&q=80"))
                .build(),
            Product.builder()
                .name("Bridal Red Banarasi Saree")
                .description("Magnificent bridal Banarasi saree with heavy gold zari, perfect for the wedding day.")
                .price(new BigDecimal("28900.00"))
                .category(bridal)
                .stockQuantity(10)
                .fabric("Silk")
                .occasion("Wedding")
                .color("Red")
                .images(List.of("https://images.unsplash.com/photo-1605721911519-3dfeb3be25e7?auto=format&fit=crop&fm=webp&w=800&q=80"))
                .build(),
            Product.builder()
                .name("Indo-Western Designer Saree")
                .description("Modern indo-western designer saree with contemporary drape style and pre-stitched pallu.")
                .price(new BigDecimal("13200.00"))
                .category(designer)
                .stockQuantity(18)
                .fabric("Georgette")
                .occasion("Party")
                .color("Navy Blue")
                .images(List.of("https://images.unsplash.com/photo-1583391733956-3750e0ff4e8b?auto=format&fit=crop&fm=webp&w=800&q=80"))
                .build(),
            Product.builder()
                .name("Mangalagiri Cotton Saree")
                .description("Traditional Mangalagiri handloom cotton saree with zari border. Lightweight and comfortable.")
                .price(new BigDecimal("899.00"))
                .category(cotton)
                .stockQuantity(60)
                .fabric("Cotton")
                .occasion("Daily Wear")
                .color("Yellow")
                .images(List.of("https://images.unsplash.com/photo-1583391265517-35bbdad01209?auto=format&fit=crop&fm=webp&w=800&q=80"))
                .build(),
            Product.builder()
                .name("Patola Silk Double Ikat Saree")
                .description("Rare Patola silk saree with double ikat weave from Gujarat. A collector's piece.")
                .price(new BigDecimal("38500.00"))
                .category(silk)
                .stockQuantity(5)
                .fabric("Patola Silk")
                .occasion("Wedding")
                .color("Multi")
                .images(List.of("https://images.unsplash.com/photo-1617627143233-46b92015e905?auto=format&fit=crop&fm=webp&w=800&q=80"))
                .build(),
            Product.builder()
                .name("Chanderi Tissue Zari Floral Saree")
                .description("Gossamer Chanderi tissue silk saree woven with gold coin buttis and translucent silver selvage.")
                .price(new BigDecimal("11800.00"))
                .category(cotton)
                .stockQuantity(11)
                .fabric("Chanderi Silk")
                .occasion("Party Wear")
                .color("Champagne Silver")
                .images(List.of("https://images.unsplash.com/photo-1507679799987-c73779587ccf?auto=format&fit=crop&fm=webp&w=800&q=80"))
                .build(),
            Product.builder()
                .name("Tussar Hand-Block Printed Raw Silk Saree")
                .description("Rich wild tussar silk saree with authentic Ajrakh hand-block printing and organic vegetable dyes.")
                .price(new BigDecimal("15400.00"))
                .category(silk)
                .stockQuantity(6)
                .fabric("Tussar Silk")
                .occasion("Casual")
                .color("Terracotta Earth")
                .images(List.of("https://images.unsplash.com/photo-1605647540924-852290f6b0d5?auto=format&fit=crop&fm=webp&w=800&q=80"))
                .build()
        );
        productRepository.saveAll(products);
    }

    private void seedCoupons() {
        Coupon c1 = Coupon.builder()
                .code("WELCOME10")
                .discountPercent(10.0)
                .active(true)
                .expiryDate(LocalDateTime.now().plusMonths(6))
                .build();

        Coupon c2 = Coupon.builder()
                .code("WEDDING20")
                .discountPercent(20.0)
                .active(true)
                .expiryDate(LocalDateTime.now().plusMonths(6))
                .build();

        couponRepository.saveAll(List.of(c1, c2));
        log.info("Promo coupons WELCOME10 and WEDDING20 seeded successfully!");
    }

    @org.springframework.transaction.annotation.Transactional
    private void seedHistoricalOrders() {
        List<Product> products = productRepository.findAll();
        if (products.isEmpty()) return;

        User customer = userRepository.findByEmail("customer@sareekart.com").orElse(null);
        if (customer == null) return;

        User customer2 = userRepository.findByEmail("priya@example.com").orElseGet(() -> {
            User u = User.builder()
                    .firstName("Priya")
                    .lastName("Sharma")
                    .email("priya@example.com")
                    .mobile("9876543220")
                    .password(passwordEncoder.encode("customer123"))
                    .role(com.sareekart.entity.Role.CUSTOMER)
                    .build();
            return userRepository.save(u);
        });

        User customer3 = userRepository.findByEmail("ananya@example.com").orElseGet(() -> {
            User u = User.builder()
                    .firstName("Ananya")
                    .lastName("Verma")
                    .email("ananya@example.com")
                    .mobile("9876543221")
                    .password(passwordEncoder.encode("customer123"))
                    .role(com.sareekart.entity.Role.CUSTOMER)
                    .build();
            return userRepository.save(u);
        });

        List<User> customers = List.of(customer, customer2, customer3);

        String[][] locations = {
            {"Bengaluru", "Karnataka", "560001"},
            {"Mumbai", "Maharashtra", "400001"},
            {"Delhi", "Delhi", "110001"},
            {"Hyderabad", "Telangana", "500001"},
            {"Chennai", "Tamil Nadu", "600001"}
        };

        LocalDateTime now = LocalDateTime.now();

        int[] daysAgoOffsets = {
            0, 0, 1, 1, 2, 3, 4, 5, 6, 7, 8, 10, 12, 14, 16, 18, 20, 22, 24, 26, 28, 30,
            33, 36, 40, 45, 50, 55, 60, 65, 70, 75, 80, 85, 88
        };

        for (int i = 0; i < daysAgoOffsets.length; i++) {
            int daysAgo = daysAgoOffsets[i];
            LocalDateTime orderDate = now.minusDays(daysAgo).minusHours(i % 12).minusMinutes((i * 7) % 60);

            User user = customers.get(i % customers.size());
            String[] loc = locations[i % locations.length];

            com.sareekart.entity.Address shippingAddress = com.sareekart.entity.Address.builder()
                    .fullName(user.getFirstName() + " " + user.getLastName())
                    .phone(user.getMobile() != null ? user.getMobile() : "9876543210")
                    .streetAddress("Flat " + (100 + i) + ", Residency Road")
                    .city(loc[0])
                    .state(loc[1])
                    .pincode(loc[2])
                    .build();

            Product p1 = products.get(i % products.size());
            Product p2 = products.get((i + 3) % products.size());

            int qty1 = (i % 2) + 1;
            BigDecimal item1Total = p1.getPrice().multiply(BigDecimal.valueOf(qty1));

            com.sareekart.entity.Order order = com.sareekart.entity.Order.builder()
                    .user(user)
                    .shippingAddress(shippingAddress)
                    .paymentMethod(i % 2 == 0 ? "RAZORPAY" : "COD")
                    .paymentStatus(i == 15 ? "REFUNDED/CANCELLED" : "COMPLETED")
                    .status(i == 15 ? com.sareekart.entity.OrderStatus.CANCELLED : com.sareekart.entity.OrderStatus.DELIVERED)
                    .build();

            List<com.sareekart.entity.OrderItem> items = new java.util.ArrayList<>();
            com.sareekart.entity.OrderItem item1 = com.sareekart.entity.OrderItem.builder()
                    .order(order)
                    .product(p1)
                    .productName(p1.getName())
                    .productImage(p1.getImages() != null && !p1.getImages().isEmpty() ? p1.getImages().get(0) : null)
                    .quantity(qty1)
                    .price(p1.getPrice())
                    .build();
            items.add(item1);

            BigDecimal subtotal = item1Total;
            if (i % 3 == 0 && !p1.getId().equals(p2.getId())) {
                int qty2 = 1;
                items.add(com.sareekart.entity.OrderItem.builder()
                        .order(order)
                        .product(p2)
                        .productName(p2.getName())
                        .productImage(p2.getImages() != null && !p2.getImages().isEmpty() ? p2.getImages().get(0) : null)
                        .quantity(qty2)
                        .price(p2.getPrice())
                        .build());
                subtotal = subtotal.add(p2.getPrice().multiply(BigDecimal.valueOf(qty2)));
            }

            BigDecimal discount = BigDecimal.ZERO;
            if (i % 4 == 0) {
                discount = subtotal.multiply(BigDecimal.valueOf(0.10));
            } else if (i % 7 == 0) {
                discount = subtotal.multiply(BigDecimal.valueOf(0.20));
            }

            BigDecimal shipping = subtotal.compareTo(BigDecimal.valueOf(5000)) >= 0 ? BigDecimal.ZERO : BigDecimal.valueOf(150);
            BigDecimal totalAmount = subtotal.subtract(discount).add(shipping);

            order.setItems(items);
            order.setTotalAmount(totalAmount);

            com.sareekart.entity.Order savedOrder = orderRepository.save(order);

            if (entityManager != null) {
                entityManager.createNativeQuery("UPDATE orders SET created_at = :orderDate, updated_at = :orderDate WHERE id = :id")
                        .setParameter("orderDate", orderDate)
                        .setParameter("id", savedOrder.getId())
                        .executeUpdate();
            }
        }
    }
}
