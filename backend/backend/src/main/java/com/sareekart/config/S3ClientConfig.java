package com.sareekart.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

/**
 * S3 client configuration. Uses explicit credentials when provided via env
 * vars; falls back to the default AWS credential chain (IAM role, etc).
 */
@Configuration
@ConditionalOnProperty(prefix = "app.storage", name = "provider", havingValue = "s3")
public class S3ClientConfig {

    @Bean
    public S3Client s3Client(
            @Value("${AWS_REGION:us-east-1}") String region,
            @Value("${AWS_ACCESS_KEY_ID:}") String accessKey,
            @Value("${AWS_SECRET_ACCESS_KEY:}") String secretKey,
            @Value("${app.storage.endpoint:}") String endpoint) {

        var builder = S3Client.builder().region(Region.of(region));

        if (!accessKey.isBlank() && !secretKey.isBlank()) {
            builder.credentialsProvider(
                StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)));
        }
        // else: default credential chain (IAM role, ~/.aws/credentials, etc)

        if (!endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint));
        }

        return builder.build();
    }
}
