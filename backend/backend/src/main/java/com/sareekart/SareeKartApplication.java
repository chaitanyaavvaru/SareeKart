package com.sareekart;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication(exclude = {org.springframework.cloud.function.context.config.ContextFunctionCatalogAutoConfiguration.class})
@EnableJpaAuditing
public class SareeKartApplication {
    public static void main(String[] args) {
        SpringApplication.run(SareeKartApplication.class, args);
    }
}
