package com.sareekart.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * Serves locally uploaded saree photos as static resources.
 * Files saved to  uploads/saree-photos/  are accessible at
 *   http://localhost:8081/uploads/saree-photos/<filename>
 */
@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String uploadPath = Paths.get("uploads").toAbsolutePath().toUri().toString();
        if (!uploadPath.endsWith("/")) {
            uploadPath += "/";
        }
        registry
            .addResourceHandler("/uploads/**")
            .addResourceLocations(uploadPath);
    }
}
