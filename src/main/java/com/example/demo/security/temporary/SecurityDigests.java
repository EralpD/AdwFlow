package com.example.demo.security.temporary;

import org.springframework.stereotype.Component;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

@Component
public final class SecurityDigests {
    private final SecretKeySpec key;
    private final SecureRandom random = new SecureRandom();

    public SecurityDigests(RedisSecurityProperties properties) {
        key = new SecretKeySpec(Base64.getDecoder().decode(properties.hmacSecret()), "HmacSHA256");
    }

    public String randomToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public String verificationCode() { return String.format(Locale.ROOT, "%06d", random.nextInt(1_000_000)); }

    public String digest(String... parts) {
        try {
            // Mac is not thread-safe. Each operation has its own instance.
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(key);
            for (String part : parts) {
                byte[] bytes = part.getBytes(StandardCharsets.UTF_8);
                mac.update(Integer.toString(bytes.length).getBytes(StandardCharsets.US_ASCII));
                mac.update((byte) ':');
                mac.update(bytes);
            }
            return HexFormat.of().formatHex(mac.doFinal());
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("HMAC-SHA256 is unavailable.");
        }
    }
}
