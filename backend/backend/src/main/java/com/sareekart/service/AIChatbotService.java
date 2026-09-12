package com.sareekart.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIChatbotService {

    private final ChatClient chatClient;
    private final WhatsAppApiClient whatsAppApiClient;

    public void handleIncomingMessage(String phoneNumber, String userMessage) {
        log.info("Processing AI response for {} : {}", phoneNumber, userMessage);

        try {
            String aiResponse = chatClient.prompt()
                    .system("You are a friendly AI assistant for SareeKart, an artisanal luxury saree e-commerce store. " +
                            "Greet customers warmly. Use the provided tools to search for products (like Banarasi sarees under 5000) when asked. " +
                            "Provide concise, helpful answers. Format your text nicely with emojis. " +
                            "If sharing a product, include its name, price, and tell the user they can view more on our website.")
                    .user(userMessage)
                    .functions("searchProducts") // Matches the bean name in AIToolConfig
                    .call()
                    .content();

            log.info("AI replied to {}: {}", phoneNumber, aiResponse);
            
            // Send back to user via WhatsApp
            whatsAppApiClient.sendTextMessage(phoneNumber, aiResponse);
            
        } catch (Exception e) {
            log.error("Failed to get AI response for {}", phoneNumber, e);
            whatsAppApiClient.sendTextMessage(phoneNumber, "Sorry, I am having trouble connecting to my brain right now! Please try again later.");
        }
    }
}
