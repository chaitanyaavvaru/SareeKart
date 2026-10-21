package com.sareekart.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Spring configuration for AWS S3 Client and Storage domain.
 * Configured with lightweight URLConnection HTTP client to minimize heap/thread overhead on ECS Fargate.
 */
@Configuration
@Slf4j
public class StorageConfig {

    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(name = "sareekart.storage.type", havingValue = "s3")
    public S3Client s3Client(@Value("${sareekart.storage.s3.region:ap-south-1}") String region) {
        log.info("Initializing AWS SDK v2 S3 Client for region {} using Task Role DefaultCredentialsProvider", region);
        return S3Client.builder()
                .region(Region.of(region))
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }
}
