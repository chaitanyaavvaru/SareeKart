package com.example.backend.controller;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "*")
public class ProductController {

    @GetMapping
    public List<Map<String, Object>> getProducts() {

        List<Map<String, Object>> products = new ArrayList<>();

        Map<String, Object> p1 = new HashMap<>();
        p1.put("id", 1);
        p1.put("name", "Banarasi Silk Saree");
        p1.put("price", 4999);
        p1.put("category", "Banarasi");

        Map<String, Object> p2 = new HashMap<>();
        p2.put("id", 2);
        p2.put("name", "Kanchipuram Silk Saree");
        p2.put("price", 6499);
        p2.put("category", "Kanchipuram");

        products.add(p1);
        products.add(p2);

        return products;
    }
}