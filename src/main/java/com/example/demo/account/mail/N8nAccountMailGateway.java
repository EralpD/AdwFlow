package com.example.demo.account.mail;

import com.example.demo.account.AccountRecoveryService;
import com.example.demo.security.temporary.ChallengePurpose;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;
import java.util.UUID;

@Service
public final class N8nAccountMailGateway implements AccountMailGateway {
    private static final Logger log = LoggerFactory.getLogger(N8nAccountMailGateway.class);
    private final N8nMailProperties properties;
    private final RestClient client;

    public N8nAccountMailGateway(N8nMailProperties properties) {
        this.properties = properties;
        var requests = new SimpleClientHttpRequestFactory();
        requests.setConnectTimeout(properties.connectTimeout());
        requests.setReadTimeout(properties.readTimeout());
        this.client = RestClient.builder().requestFactory(requests).build();
    }

    @Override
    public boolean deliver(AccountRecoveryService.Delivery delivery) {
        if (!properties.enabled()) {
            log.warn("Account email delivery was requested while the n8n mail gateway is disabled.");
            return false;
        }
        var challenge = delivery.challenge();
        String verificationUrl = null;
        String resetUrl = null;
        String code = null;
        if (challenge.purpose() == ChallengePurpose.EMAIL_VERIFICATION) {
            code = challenge.secret();
            verificationUrl = publicUrl("verify-email", challenge.challengeId(), null);
        } else {
            resetUrl = publicUrl("reset-password", challenge.challengeId(), challenge.secret());
        }
        var payload = new N8nMailRequest(challenge.purpose().name(), challenge.challengeId(), delivery.email(), code,
                verificationUrl, resetUrl, challenge.validFor().toSeconds());
        return post(payload);
    }

    @Override
    public boolean notifyPasswordChanged(String recipient) {
        if (recipient == null || recipient.isBlank()) return false;
        return post(new N8nMailRequest("PASSWORD_CHANGED", UUID.randomUUID().toString(), recipient,
                null, null, null, 0));
    }

    private boolean post(N8nMailRequest payload) {
        if (!properties.enabled()) {
            log.warn("Account email delivery was requested while the n8n mail gateway is disabled.");
            return false;
        }
        try {
            client.post().uri(properties.webhookUri())
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(properties.authHeader(), properties.authSecret())
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (RestClientException exception) {
            // Never include the exception message: HTTP errors can echo credential-bearing URLs or payloads.
            log.warn("n8n account email delivery failed ({}).", exception.getClass().getSimpleName());
            return false;
        }
    }

    private String publicUrl(String path, String challengeId, String token) {
        var builder = UriComponentsBuilder.fromUri(properties.baseUri()).path(path)
                .queryParam("challenge", challengeId);
        if (token != null) builder.queryParam("token", token);
        return builder.build().encode().toUriString();
    }

    private record N8nMailRequest(String eventType, String deliveryId, String recipient, String code,
            String verificationUrl, String resetUrl, long expiresInSeconds) {}
}
