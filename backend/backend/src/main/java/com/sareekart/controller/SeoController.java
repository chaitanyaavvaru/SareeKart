package com.sareekart.controller;

import com.sareekart.service.SeoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SeoController {

    private final SeoService seoService;

    @GetMapping(value = {"/api/seo/sitemap.xml", "/sitemap.xml"}, produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> getSitemapXml() {
        String sitemap = seoService.generateSitemapXml();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "application/xml; charset=UTF-8")
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                .body(sitemap);
    }

    @GetMapping(value = {"/api/seo/robots.txt", "/robots.txt"}, produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> getRobotsTxt() {
        String robots = seoService.generateRobotsTxt();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "text/plain; charset=UTF-8")
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                .body(robots);
    }
}
