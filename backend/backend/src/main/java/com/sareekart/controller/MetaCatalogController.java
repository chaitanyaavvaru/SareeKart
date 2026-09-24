package com.sareekart.controller;

import com.sareekart.service.MetaCatalogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/meta")
@RequiredArgsConstructor
@Slf4j
public class MetaCatalogController {

    private final MetaCatalogService metaCatalogService;

    /**
     * Public read-only endpoint serving active products as an RFC 4180 CSV feed
     * for Meta Commerce Manager and Instagram Shop.
     */
    @GetMapping(value = "/catalog.csv", produces = "text/csv; charset=UTF-8")
    public ResponseEntity<byte[]> getMetaCatalogCsv() {
        log.info("Serving Meta/Instagram Commerce product catalog feed CSV");
        String csv = metaCatalogService.generateCatalogCsv();
        byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8")
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"sareekart-meta-catalog.csv\"")
                .body(bytes);
    }
}
