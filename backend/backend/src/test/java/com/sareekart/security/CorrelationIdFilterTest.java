package com.sareekart.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CorrelationIdFilterTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private CorrelationIdFilter filter;

    @BeforeEach
    @AfterEach
    void clearMdc() {
        MDC.remove(CorrelationIdFilter.MDC_KEY);
    }

    @Test
    @DisplayName("doFilterInternal sets MDC, header, and clears MDC after successful chain execution")
    void testDoFilterInternal_Success_SetsAndClearsMdc() throws ServletException, IOException {
        String clientRequestId = "req-client-trace-999";
        when(request.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER)).thenReturn(clientRequestId);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/products");
        when(response.getStatus()).thenReturn(200);

        // Verify MDC is populated during filter chain execution
        doAnswer(invocation -> {
            assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isEqualTo(clientRequestId);
            return null;
        }).when(filterChain).doFilter(request, response);

        filter.doFilter(request, response, filterChain);

        verify(response).setHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, clientRequestId);
        verify(filterChain).doFilter(request, response);
        // Verify MDC was reliably cleared in finally block
        assertNull(MDC.get(CorrelationIdFilter.MDC_KEY), "MDC must be cleared after filter completion");
    }

    @Test
    @DisplayName("doFilterInternal reliably clears MDC even when filter chain throws an unhandled exception")
    void testDoFilterInternal_ExceptionInChain_ClearsMdc() throws ServletException, IOException {
        String clientRequestId = "req-faulty-chain-123";
        when(request.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER)).thenReturn(clientRequestId);
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/checkout");
        when(response.getStatus()).thenReturn(500);

        doAnswer(invocation -> {
            assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isEqualTo(clientRequestId);
            throw new RuntimeException("Simulated unexpected downstream error");
        }).when(filterChain).doFilter(request, response);

        assertThrows(RuntimeException.class, () -> filter.doFilter(request, response, filterChain));

        verify(response).setHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, clientRequestId);
        assertNull(MDC.get(CorrelationIdFilter.MDC_KEY), "MDC must be reliably removed in finally block on error");
    }

    @Test
    @DisplayName("doFilterInternal sanitizes invalid characters in inbound X-Request-ID to safe UUID")
    void testDoFilterInternal_SanitizesUnsafeHeader() throws ServletException, IOException {
        String unsafeHeader = "<script>alert('xss')</script>\r\nInjected: True";
        when(request.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER)).thenReturn(unsafeHeader);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/health");
        when(response.getStatus()).thenReturn(200);

        doAnswer(invocation -> {
            String mdcValue = MDC.get(CorrelationIdFilter.MDC_KEY);
            assertNotNull(mdcValue);
            assertNotEquals(unsafeHeader, mdcValue);
            assertTrue(mdcValue.matches("^[a-zA-Z0-9_-]{1,64}$"), "Sanitized ID must match safe regex");
            return null;
        }).when(filterChain).doFilter(request, response);

        filter.doFilter(request, response, filterChain);

        assertNull(MDC.get(CorrelationIdFilter.MDC_KEY));
    }
}
