package com.sareekart.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * Serves locally uploaded public saree photos as static resources in local development.
 * Restricted STRICTLY to public catalog media (/uploads/saree-photos/**) to ensure
 * private return photos (/uploads/return-photos/**) can NEVER bypass MediaServingController authorization.
 * Completely disabled when sareekart.storage.type=s3.
 */
@Configuration
@ConditionalOnProperty(name = "sareekart.storage.type", havingValue = "local", matchIfMissing = true)
public class StaticResourceConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String uploadPath = Paths.get("uploads", "saree-photos").toAbsolutePath().toUri().toString();
        if (!uploadPath.endsWith("/")) {
            uploadPath += "/";
        }
        registry
            .addResourceHandler("/uploads/saree-photos/**")
            .addResourceLocations(uploadPath);
    }
}
