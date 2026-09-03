package com.example.demo.account.mail;

import org.junit.jupiter.api.Test;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class N8nMailPropertiesTest {
    @Test
    void disabledGatewayDoesNotRequireWebhookCredentials() {
        var properties = new N8nMailProperties(false, "", "X-AdwFlow-Webhook-Secret", "",
                "http://localhost:8080", Duration.ofSeconds(2), Duration.ofSeconds(8));
        assertThat(properties.enabled()).isFalse();
        assertThat(properties.baseUri().toString()).isEqualTo("http://localhost:8080/");
    }

    @Test
    void enabledGatewayRequiresAStrongSecretAndAbsoluteWebhook() {
        assertThatIllegalArgumentException().isThrownBy(() -> new N8nMailProperties(true, "not-a-url",
                "X-AdwFlow-Webhook-Secret", "short", "https://app.example.com",
                Duration.ofSeconds(2), Duration.ofSeconds(8)));
    }
}
