package com.sareekart;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = {org.springframework.cloud.function.context.config.ContextFunctionCatalogAutoConfiguration.class})
@EnableJpaAuditing
@EnableScheduling
@EnableAsync
public class SareeKartApplication {
    public static void main(String[] args) {
        SpringApplication.run(SareeKartApplication.class, args);
    }
}
