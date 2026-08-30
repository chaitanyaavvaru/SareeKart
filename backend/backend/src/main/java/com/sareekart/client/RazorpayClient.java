package com.sareekart.client;

import com.sareekart.config.RazorpayProperties;
import com.sareekart.dto.payment.RazorpayOrderResponse;
import com.sareekart.dto.payment.RazorpayRefundResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;

/**
 * Production adapter for the Razorpay Orders API.
 *
 * Uses Spring's RestClient over a pooled JDK HttpClient. Credentials travel
 * only via Basic Auth on outbound calls; they are never logged or returned
 * in any API response.
 */
public class RazorpayClient implements RazorpayGateway {

    private final RestClient restClient;
    private final String keyId;

    public RazorpayClient(RazorpayProperties properties) {
        this.keyId = properties.getKeyId();
        String credentials = Base64.getEncoder().encodeToString(
            (properties.getKeyId() + ":" + properties.getKeySecret())
                .getBytes(StandardCharsets.UTF_8));

        HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

        this.restClient = RestClient.builder()
            .requestFactory(new JdkClientHttpRequestFactory(httpClient))
            .baseUrl(properties.getBaseUrl())
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + credentials)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .build();
    }

    @Override
    public RazorpayOrderResponse createOrder(String receiptOrderId, long amountRupees, String currency) {
        long amountPaise = amountRupees * 100; // Razorpay expects paise

        Map<String, Object> requestBody = Map.of(
            "amount", amountPaise,
            "currency", currency != null ? currency : "INR",
            "receipt", receiptOrderId,
            "payment_capture", 1 // auto-capture
        );

        return restClient.post()
            .uri("/orders")
            .body(requestBody)
            .retrieve()
            .body(RazorpayOrderResponse.class);
    }

    /**
     * POST /payments/{id}/refund — full or partial (paise). The Key Secret
     * authenticates the call but is never logged or returned.
     */
    @Override
    public RazorpayRefundResponse createRefund(String razorpayPaymentId, long amountPaise) {
        return restClient.post()
            .uri("/payments/{id}/refund", razorpayPaymentId)
            .body(Map.of("amount", amountPaise))
            .retrieve()
            .body(RazorpayRefundResponse.class);
    }

    /**
     * GET /refunds/{id}. HTTP 404 maps to Optional.empty(); any other error
     * propagates as RestClientException so callers apply retry backoff.
     */
    @Override
    public java.util.Optional<RazorpayRefundResponse> fetchRefund(String providerRefundId) {
        try {
            return java.util.Optional.ofNullable(restClient.get()
                .uri("/refunds/{id}", providerRefundId)
                .retrieve()
                .body(RazorpayRefundResponse.class));
        } catch (org.springframework.web.client.HttpClientErrorException.NotFound notFound) {
            return java.util.Optional.empty();
        }
    }

    /** Key ID is public by design (embedded in frontend checkout); secret never leaves this class. */
    public String getPublicKeyId() {
        return keyId;
    }
}