package com.sareekart.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.filter.CorsFilter;

import static org.assertj.core.api.Assertions.assertThat;

class CorsConfigTest {

    @Test
    void allowsTheIpv4ViteDevelopmentOrigin() throws Exception {
        CorsFilter filter = new CorsConfig().corsFilter();
        MockHttpServletRequest request = preflightRequest("http://127.0.0.1:5173");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getHeader("Access-Control-Allow-Origin"))
                .isEqualTo("http://127.0.0.1:5173");
    }

    @Test
    void rejectsAnUnlistedOrigin() throws Exception {
        CorsFilter filter = new CorsConfig().corsFilter();
        MockHttpServletRequest request = preflightRequest("http://malicious.example");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getHeader("Access-Control-Allow-Origin")).isNull();
    }

    @Test
    void allowsWildcardVercelSubdomainsWithCredentials() throws Exception {
        CorsFilter filter = new CorsConfig("https://sareekart.com,https://*.vercel.app").corsFilter();
        MockHttpServletRequest request = preflightRequest("https://sareekart-git-preview-123.vercel.app");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getHeader("Access-Control-Allow-Origin"))
                .isEqualTo("https://sareekart-git-preview-123.vercel.app");
        assertThat(response.getHeader("Access-Control-Allow-Credentials"))
                .isEqualTo("true");
    }

    @Test
    void allowsProductionCanonicalDomainWithCredentials() throws Exception {
        CorsFilter filter = new CorsConfig("https://sareekart.com,https://*.vercel.app").corsFilter();
        MockHttpServletRequest request = preflightRequest("https://sareekart.com");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getHeader("Access-Control-Allow-Origin"))
                .isEqualTo("https://sareekart.com");
        assertThat(response.getHeader("Access-Control-Allow-Credentials"))
                .isEqualTo("true");
    }

    @Test
    void rejectsMismatchedDomainWithWildcardPattern() throws Exception {
        CorsFilter filter = new CorsConfig("https://sareekart.com,https://*.vercel.app").corsFilter();
        MockHttpServletRequest request = preflightRequest("https://attacker-vercel.app.malicious.com");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getHeader("Access-Control-Allow-Origin")).isNull();
    }

    private MockHttpServletRequest preflightRequest(String origin) {
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/auth/register");
        request.addHeader("Origin", origin);
        request.addHeader("Access-Control-Request-Method", "POST");
        request.addHeader("Access-Control-Request-Headers", "content-type");
        return request;
    }
}
