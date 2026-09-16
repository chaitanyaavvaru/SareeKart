package com.sareekart.service.impl;

import com.sareekart.dto.internal.StylistIntent;
import com.sareekart.dto.request.StylistChatMessage;
import com.sareekart.dto.request.StylistChatRequest;
import com.sareekart.dto.whatsapp.WhatsAppSessionContext;
import com.sareekart.entity.User;
import com.sareekart.entity.WhatsAppContact;
import com.sareekart.repository.UserRepository;
import com.sareekart.service.*;
import com.sareekart.service.whatsapp.WhatsAppCommerceTools;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class WhatsAppAiCommerceServiceImpl implements WhatsAppAiCommerceService {

    private final WhatsAppIdentityService identityService;
    private final WhatsAppConversationStateManager stateManager;
    private final WhatsAppCommerceTools commerceTools;
    private final WhatsAppApiClient whatsAppApiClient;
    private final StylistIntentExtractor intentExtractor;
    private final UserRepository userRepository;

    @Autowired(required = false)
    private ChatClient chatClient;

    private final ExecutorService aiExecutor = Executors.newFixedThreadPool(4);

    @Override
    public void processIncomingMessage(String phoneNumber, String messageContent, Long conversationId) {
        if (phoneNumber == null || messageContent == null) return;
        String normalizedPhone = identityService.normalizePhoneNumber(phoneNumber);
        String text = messageContent.trim();
        String lower = text.toLowerCase();

        log.info("Processing WhatsApp commerce message from {}: '{}'", normalizedPhone, text);

        // 1. Resolve Identity and Context
        WhatsAppContact contact = identityService.resolveContact(normalizedPhone, null);
        User user = identityService.getLinkedUser(contact);
        WhatsAppSessionContext session = stateManager.getSession(normalizedPhone);
        session.setConversationId(conversationId);

        // 2. Compliance: Opt-Out / Opt-In
        if (lower.equals("stop") || lower.equals("unsubscribe")) {
            if (user != null) {
                user.setWhatsappOptIn(false);
                userRepository.save(user);
            }
            stateManager.clearSession(normalizedPhone);
            whatsAppApiClient.sendTextMessage(normalizedPhone, 
                    "You have been unsubscribed from SareeKart updates. Reply START anytime to reconnect with our atelier. 🙏");
            return;
        }

        if (lower.equals("start")) {
            if (user != null) {
                user.setWhatsappOptIn(true);
                userRepository.save(user);
            }
            whatsAppApiClient.sendTextMessage(normalizedPhone, 
                    "Namaste! Welcome back to SareeKart. 🙏 How may our styling atelier assist you today?");
            return;
        }

        // 3. Fast-Path: Human Escalation
        if (lower.contains("human") || lower.contains("agent") || lower.contains("support") 
                || lower.contains("call me") || lower.contains("talk to someone")) {
            WhatsAppCommerceTools.HumanHandoffResult result = commerceTools.escalateToHuman(
                    new WhatsAppCommerceTools.HumanHandoffInput(conversationId, text)
            );
            whatsAppApiClient.sendTextMessage(normalizedPhone, result.message());
            return;
        }

        // 4. Fast-Path: Order Tracking
        if (lower.contains("track") || lower.contains("order status") || lower.contains("where is my order")) {
            handleOrderTrackingFastPath(normalizedPhone, user, text);
            return;
        }

        // 5. Fast-Path: Greetings & Main Menu
        if (lower.equals("hi") || lower.equals("hello") || lower.equals("namaste") 
                || lower.equals("menu") || lower.equals("help")) {
            sendWelcomeMenu(normalizedPhone, user);
            return;
        }

        // 6. Fast-Path: Cart Interaction
        if (lower.contains("view cart") || lower.contains("my cart") || lower.contains("show cart")) {
            handleCartView(normalizedPhone, user);
            return;
        }

        if (lower.contains("add to cart") || lower.contains("buy this")) {
            handleAddToCart(normalizedPhone, user, session);
            return;
        }

        // 7. Conversational Product Discovery & AI Styling Flow
        handleConversationalStyling(normalizedPhone, user, text, session);
    }

    private void sendWelcomeMenu(String phoneNumber, User user) {
        String greeting = user != null ? "Namaste, " + user.getFirstName() + "!" : "Namaste!";
        String body = greeting + " Welcome to SareeKart's Luxury Handloom Atelier. ✨\n\n"
                + "I can help you discover heirloom Kanchipuram and Banarasi silks, curate bespoke contrast blouse coordinates, check order deliveries, or connect you with a boutique curator.";

        List<Map<String, String>> buttons = List.of(
                Map.of("id", "btn_style", "title", "👗 Style Ensemble"),
                Map.of("id", "btn_trending", "title", "✨ Trending Sarees"),
                Map.of("id", "btn_track", "title", "📦 Track Order")
        );
        whatsAppApiClient.sendInteractiveButtonsMessage(phoneNumber, body, buttons);
    }

    private void handleOrderTrackingFastPath(String phoneNumber, User user, String text) {
        Long userId = user != null ? user.getId() : null;
        Long orderId = null;

        // Extract any numeric sequence that looks like an order ID
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\b(\\d{3,8})\\b").matcher(text);
        if (m.find()) {
            try {
                orderId = Long.parseLong(m.group(1));
            } catch (NumberFormatException ignored) {}
        }

        WhatsAppCommerceTools.OrderTrackingResult result = commerceTools.trackOrder(
                new WhatsAppCommerceTools.OrderTrackingInput(orderId, null, userId, phoneNumber)
        );

        if (result.found()) {
            String msg = "📦 *Order Status Update*\n"
                    + "• Status: *" + result.orderStatus() + "*\n"
                    + "• Courier: " + result.courierPartner() + " (" + result.trackingNumber() + ")\n"
                    + "• Expected Delivery: " + result.estimatedDelivery() + "\n\n"
                    + "🔗 Live Tracking: " + result.trackingUrl();
            whatsAppApiClient.sendTextMessage(phoneNumber, msg);
        } else {
            if (user == null) {
                whatsAppApiClient.sendTextMessage(phoneNumber, 
                        "To track your order, please provide your Order ID (e.g. 'track 101') or link your account at https://sareekart.com/login");
            } else {
                whatsAppApiClient.sendTextMessage(phoneNumber, 
                        "No active orders found matching your profile. Please check your Order ID or visit https://sareekart.com/orders");
            }
        }
    }

    private void handleCartView(String phoneNumber, User user) {
        if (user == null) {
            whatsAppApiClient.sendTextMessage(phoneNumber, 
                    "To view your personal shopping cart, please sign in at: https://sareekart.com/login");
            return;
        }

        WhatsAppCommerceTools.CartActionResult result = commerceTools.manageCart(
                new WhatsAppCommerceTools.CartActionInput("VIEW", null, null, user.getId())
        );

        String msg = "🛍️ *Your SareeKart Cart*\n"
                + "• Total Items: " + result.totalItems() + "\n"
                + "• Estimated Total: ₹" + result.totalAmount() + "\n\n"
                + "🔗 Proceed to Secure Checkout:\nhttps://sareekart.com/checkout";

        List<Map<String, String>> buttons = List.of(
                Map.of("id", "btn_checkout", "title", "💳 Checkout Web"),
                Map.of("id", "btn_style", "title", "👗 Continue Styling")
        );
        whatsAppApiClient.sendInteractiveButtonsMessage(phoneNumber, msg, buttons);
    }

    private void handleAddToCart(String phoneNumber, User user, WhatsAppSessionContext session) {
        if (user == null) {
            whatsAppApiClient.sendTextMessage(phoneNumber, 
                    "Please link your SareeKart account to save items to your personal cart: https://sareekart.com/login");
            return;
        }

        Long productId = session.getLastReferencedProductId();
        if (productId == null) {
            whatsAppApiClient.sendTextMessage(phoneNumber, 
                    "Which saree would you like to add? Please search or select a saree first!");
            return;
        }

        WhatsAppCommerceTools.CartActionResult result = commerceTools.manageCart(
                new WhatsAppCommerceTools.CartActionInput("ADD", productId, 1, user.getId())
        );

        if (result.success()) {
            String msg = "✅ *Added to Your Cart!*\n"
                    + "Your cart now has " + result.totalItems() + " item(s) (Total: ₹" + result.totalAmount() + ").\n\n"
                    + "🔗 Complete your purchase:\nhttps://sareekart.com/checkout";

            List<Map<String, String>> buttons = List.of(
                    Map.of("id", "btn_checkout", "title", "💳 Proceed to Buy"),
                    Map.of("id", "btn_tailoring", "title", "✂️ Customize Blouse")
            );
            whatsAppApiClient.sendInteractiveButtonsMessage(phoneNumber, msg, buttons);
        } else {
            whatsAppApiClient.sendTextMessage(phoneNumber, "Could not add item to cart: " + result.message());
        }
    }

    private void handleConversationalStyling(String phoneNumber, User user, String text, WhatsAppSessionContext session) {
        // Step 1: Extract intent from current turn
        StylistChatRequest req = StylistChatRequest.builder()
                .message(text)
                .occasion(session.getActiveOccasion())
                .preferredWeave(session.getPreferredFabric())
                .budgetRange(session.getMaxBudget() != null ? "UNDER_" + session.getMaxBudget() : null)
                .build();

        StylistIntent intent = intentExtractor.extractIntent(text, req);

        // Update multi-turn session attributes
        if (intent.getOccasion() != null) session.setActiveOccasion(intent.getOccasion());
        if (intent.getPreferredFabric() != null) session.setPreferredFabric(intent.getPreferredFabric());
        if (intent.getPreferredColor() != null) session.setPreferredColor(intent.getPreferredColor());
        if (intent.getMaxPrice() != null) session.setMaxBudget(intent.getMaxPrice());

        // Step 2: Query grounded catalog candidates
        WhatsAppCommerceTools.CatalogSearchResult catalogResult = commerceTools.searchCatalog(
                new WhatsAppCommerceTools.CatalogSearchInput(
                        null,
                        session.getPreferredFabric(),
                        session.getPreferredColor(),
                        session.getActiveOccasion(),
                        session.getMaxBudget()
                )
        );

        if (catalogResult.items().isEmpty()) {
            whatsAppApiClient.sendTextMessage(phoneNumber, 
                    "We could not find matching sarees for those exact filters. Would you like to explore our latest bridal silk collection or speak with a curator?");
            return;
        }

        // Top candidate saree
        WhatsAppCommerceTools.CatalogSearchItem topSaree = catalogResult.items().get(0);
        session.setLastReferencedProductId(topSaree.id());

        // Step 3: Get Phase 9 Styling Ensemble
        WhatsAppCommerceTools.StylingEnsembleResult ensemble = commerceTools.getStylingEnsemble(
                new WhatsAppCommerceTools.StylingEnsembleInput(topSaree.id(), session.getActiveOccasion(), text),
                user
        );
        session.setActiveConsultationId(ensemble.consultationId());

        // Update turn history
        session.getRecentTurns().add(new StylistChatMessage("user", text, LocalDateTime.now()));
        session.getRecentTurns().add(new StylistChatMessage("assistant", ensemble.replyText(), LocalDateTime.now()));
        stateManager.updateSession(phoneNumber, session);

        // Step 4: Format luxury WhatsApp message
        StringBuilder sb = new StringBuilder();
        sb.append("✨ *SareeKart Haute Couture Recommendation*\n\n");
        sb.append("• *Saree*: ").append(topSaree.name()).append(" — *₹").append(topSaree.price()).append("*\n");
        sb.append("• *Fabric*: ").append(topSaree.fabric()).append(" (").append(topSaree.color()).append(")\n");
        sb.append("• *Contrast Blouse*: ").append(ensemble.contrastColor()).append(" ").append(ensemble.blouseFabric())
                .append(" (").append(ensemble.frontNeck()).append(", ").append(ensemble.sleeve()).append(")\n");
        sb.append("• *Jewelry*: ").append(ensemble.jewelry()).append("\n");
        sb.append("• *Drape Style*: ").append(ensemble.drapeTechnique()).append("\n\n");
        sb.append("🔗 View Saree: ").append(topSaree.webUrl()).append("\n");

        if (ensemble.consultationId() != null) {
            sb.append("✂️ Custom Blouse Studio: https://sareekart.com/tailoring?consultationId=").append(ensemble.consultationId());
        }

        List<Map<String, String>> buttons = List.of(
                Map.of("id", "btn_add_cart", "title", "🛍️ Add to Cart"),
                Map.of("id", "btn_more", "title", "✨ More Sarees"),
                Map.of("id", "btn_human", "title", "💬 Talk to Stylist")
        );

        // Dispatch interactive response
        whatsAppApiClient.sendInteractiveButtonsMessage(phoneNumber, sb.toString(), buttons);
    }
}
