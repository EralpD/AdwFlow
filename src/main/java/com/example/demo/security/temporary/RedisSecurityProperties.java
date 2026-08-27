package com.example.demo.security.temporary;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.time.Duration;
import java.util.Base64;

@ConfigurationProperties("app.security.redis")
public record RedisSecurityProperties(String keyPrefix, String hmacSecret, boolean verifyStartup,
        Duration verificationTtl, Duration resetTtl, Duration resendCooldown, int maxAttempts,
        Duration requestWindow, int emailRequestLimit, int ipRequestLimit,
        Duration confirmWindow, int ipConfirmLimit) {
    public RedisSecurityProperties {
        if (keyPrefix == null || !keyPrefix.matches("[a-zA-Z0-9:_-]{1,80}")) {
            throw new IllegalArgumentException("Use a simple, environment-specific Redis key prefix.");
        }
        try {
            if (hmacSecret == null || Base64.getDecoder().decode(hmacSecret).length < 32) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("AUTH_HMAC_SECRET must contain at least 32 random bytes encoded as Base64.");
        }
        for (Duration duration : new Duration[]{verificationTtl, resetTtl, resendCooldown, requestWindow, confirmWindow}) {
            if (duration == null || duration.toMillis() < 1 || duration.compareTo(Duration.ofDays(1)) > 0) {
                throw new IllegalArgumentException("Security TTLs must be positive and no longer than one day.");
            }
        }
        if (maxAttempts < 1 || maxAttempts > 10 || emailRequestLimit < 1 || ipRequestLimit < 1 || ipConfirmLimit < 1) {
            throw new IllegalArgumentException("Invalid security attempt or request limits.");
        }
    }

    @Override public String toString() { return "RedisSecurityProperties[secrets redacted]"; }
}
