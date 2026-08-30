package com.sareekart.config;

import com.sareekart.client.RazorpayClient;
import com.sareekart.client.RazorpayGateway;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the Razorpay gateway adapter only when payments are enabled.
 * In COD-only mode the bean is absent and PaymentService short-circuits
 * with an explicit 503 rather than attempting live API calls.
 */
@Configuration
@ConditionalOnProperty(prefix = "app.razorpay", name = "enabled", havingValue = "true")
public class RazorpayConfig {

    @Bean
    public RazorpayGateway razorpayGateway(RazorpayProperties properties) {
        return new RazorpayClient(properties);
    }
}