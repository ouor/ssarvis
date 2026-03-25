package com.ssarvis.backend.config;

import java.net.URI;
import java.net.URISyntaxException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

@Configuration
public class StorageConfig {

    @Bean
    S3Client s3Client(AppProperties appProperties) {
        AppProperties.S3 s3 = appProperties.getStorage().getS3();
        boolean pathStyleAccess = shouldUsePathStyleAccess(s3);

        var builder = S3Client.builder()
                .region(Region.of(s3.getRegion()))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(pathStyleAccess)
                        .build());

        if (StringUtils.hasText(s3.getAccessKey()) && StringUtils.hasText(s3.getSecretKey())) {
            builder.credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(s3.getAccessKey(), s3.getSecretKey())
            ));
        } else {
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }

        if (StringUtils.hasText(s3.getEndpoint())) {
            builder.endpointOverride(URI.create(s3.getEndpoint()));
        }

        return builder.build();
    }

    public static boolean shouldUsePathStyleAccess(AppProperties.S3 s3) {
        if (s3.isPathStyleAccess()) {
            return true;
        }
        if (!StringUtils.hasText(s3.getEndpoint())) {
            return false;
        }

        try {
            URI endpointUri = new URI(s3.getEndpoint());
            String host = endpointUri.getHost();
            return "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host);
        } catch (URISyntaxException exception) {
            return false;
        }
    }
}
