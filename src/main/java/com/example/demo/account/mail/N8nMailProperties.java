package com.example.demo.account.mail;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.net.URI;
import java.time.Duration;

@ConfigurationProperties("app.mail.n8n")
public record N8nMailProperties(boolean enabled, String webhookUrl, String authHeader, String authSecret,
        String publicBaseUrl, Duration connectTimeout, Duration readTimeout) {

    public N8nMailProperties {
        connectTimeout = connectTimeout == null ? Duration.ofSeconds(2) : connectTimeout;
        readTimeout = readTimeout == null ? Duration.ofSeconds(8) : readTimeout;
        if (connectTimeout.isNegative() || connectTimeout.isZero()
                || readTimeout.isNegative() || readTimeout.isZero()) {
            throw new IllegalArgumentException("n8n timeouts must be positive.");
        }
        if (publicBaseUrl == null || !isHttpUri(publicBaseUrl)) {
            throw new IllegalArgumentException("APP_PUBLIC_BASE_URL must be an absolute HTTP(S) URL.");
        }
        if (enabled) {
            if (webhookUrl == null || !isHttpUri(webhookUrl)) {
                throw new IllegalArgumentException("N8N_MAIL_WEBHOOK_URL must be an absolute HTTP(S) URL when n8n mail is enabled.");
            }
            if (authHeader == null || !authHeader.matches("[A-Za-z0-9-]{1,80}")) {
                throw new IllegalArgumentException("N8N_MAIL_AUTH_HEADER must be a valid HTTP header name.");
            }
            if (authSecret == null || authSecret.length() < 32 || authSecret.length() > 512
                    || authSecret.chars().anyMatch(Character::isWhitespace)) {
                throw new IllegalArgumentException("N8N_MAIL_AUTH_SECRET must contain 32-512 non-whitespace characters.");
            }
        }
    }

    public URI webhookUri() { return URI.create(webhookUrl); }
    public URI baseUri() { return URI.create(publicBaseUrl.endsWith("/") ? publicBaseUrl : publicBaseUrl + "/"); }

    private static boolean isHttpUri(String value) {
        try {
            URI uri = URI.create(value);
            return uri.isAbsolute() && ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    @Override public String toString() { return "N8nMailProperties[secrets redacted]"; }
}
