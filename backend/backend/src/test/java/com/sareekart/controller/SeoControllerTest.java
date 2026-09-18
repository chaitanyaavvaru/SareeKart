package com.sareekart.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SeoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("SEO: /api/seo/sitemap.xml is publicly accessible and returns valid XML")
    void testGetSitemapXmlApi() throws Exception {
        mockMvc.perform(get("/api/seo/sitemap.xml"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
                .andExpect(xpath("/urlset").exists())
                .andExpect(xpath("/urlset/url[1]/loc").exists());
    }

    @Test
    @DisplayName("SEO: /sitemap.xml root alias is publicly accessible and returns valid XML")
    void testGetSitemapXmlRoot() throws Exception {
        mockMvc.perform(get("/sitemap.xml"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
                .andExpect(xpath("/urlset").exists());
    }

    @Test
    @DisplayName("SEO: /api/seo/robots.txt is publicly accessible and returns text/plain")
    void testGetRobotsTxtApi() throws Exception {
        mockMvc.perform(get("/api/seo/robots.txt"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("User-agent: *")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Disallow: /admin")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Sitemap: https://sareekart.com/sitemap.xml")));
    }

    @Test
    @DisplayName("SEO: /robots.txt root alias is publicly accessible and returns text/plain")
    void testGetRobotsTxtRoot() throws Exception {
        mockMvc.perform(get("/robots.txt"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("User-agent: *")));
    }
}
