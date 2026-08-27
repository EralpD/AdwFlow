package com.example.demo.security.temporary;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
@EnableConfigurationProperties(RedisSecurityProperties.class)
public class RedisSecurityConfiguration {
    @Bean
    ApplicationRunner verifySecurityRedis(StringRedisTemplate redis, RedisSecurityProperties properties) {
        return arguments -> {
            if (properties.verifyStartup()) {
                try (var connection = redis.getConnectionFactory().getConnection()) {
                    if (!"PONG".equals(connection.ping())) {
                        throw new IllegalStateException("Unexpected Redis health response.");
                    }
                } catch (RuntimeException exception) {
                    Throwable root = exception;
                    while (root.getCause() != null && root.getCause() != root) root = root.getCause();
                    // Exception types help diagnosis; messages/causes may contain a Redis URI or secret.
                    throw new IllegalStateException("Security Redis is unavailable (" + root.getClass().getSimpleName()
                            + "). Check Redis startup and connection settings.");
                }
            }
        };
    }
}
