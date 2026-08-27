package com.example.demo.security.temporary;

import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

@Service
public class RedisChallengeStore {
    private static final DefaultRedisScript<Long> ISSUE = script("issue-challenge");
    private static final DefaultRedisScript<Long> CONSUME = script("consume-challenge");
    private static final DefaultRedisScript<Long> REVOKE = script("revoke-challenge");
    private static final DefaultRedisScript<Long> RATE = script("rate-limit");
    private final StringRedisTemplate redis;
    private final RedisSecurityProperties properties;
    private final SecurityDigests digests;

    public RedisChallengeStore(StringRedisTemplate redis, RedisSecurityProperties properties, SecurityDigests digests) {
        this.redis = redis;
        this.properties = properties;
        this.digests = digests;
    }

    public Optional<IssuedChallenge> issue(ChallengePurpose purpose, long userId, long authVersion) {
        if (purpose == null || userId < 1 || authVersion < 0) throw new IllegalArgumentException("Invalid account reference.");
        String id = digests.randomToken();
        String secret = purpose == ChallengePurpose.EMAIL_VERIFICATION ? digests.verificationCode() : digests.randomToken();
        Duration ttl = purpose == ChallengePurpose.EMAIL_VERIFICATION ? properties.verificationTtl() : properties.resetTtl();
        String active = activeKey(purpose, Long.toString(userId));
        String digest = digests.digest("challenge", purpose.name(), id, secret);
        boolean issued = available(() -> success(redis.execute(ISSUE,
                List.of(challengeKey(id), active, active + ":cooldown"), id, purpose.name(), Long.toString(userId),
                Long.toString(authVersion), digest, millis(ttl), millis(properties.resendCooldown()))));
        return issued ? Optional.of(new IssuedChallenge(purpose, id, secret, ttl)) : Optional.empty();
    }

    public Optional<VerifiedAccount> consume(ChallengePurpose purpose, String challengeId, String secret) {
        if (purpose == null || !validId(challengeId)) return Optional.empty();
        // Malformed secrets count as wrong guesses, without hashing unbounded input.
        String boundedSecret = secret == null || secret.length() > 128 ? "invalid" : secret;
        return available(() -> {
            Map<Object, Object> state = redis.opsForHash().entries(challengeKey(challengeId));
            if (!purpose.name().equals(state.get("purpose"))) return Optional.empty();
            String uid = (String) state.get("uid");
            long userId = Long.parseLong(uid);
            long version = Long.parseLong((String) state.get("version"));
            // HGETALL is only a lookup. All decisive checks and deletion run together in Lua.
            boolean accepted = success(redis.execute(CONSUME,
                    List.of(challengeKey(challengeId), activeKey(purpose, uid)), challengeId, purpose.name(),
                    digests.digest("challenge", purpose.name(), challengeId, boundedSecret), Integer.toString(properties.maxAttempts())));
            return accepted ? Optional.of(new VerifiedAccount(userId, version)) : Optional.empty();
        });
    }

    public void revoke(ChallengePurpose purpose, String challengeId) {
        if (purpose == null || !validId(challengeId)) return;
        available(() -> {
            Map<Object, Object> state = redis.opsForHash().entries(challengeKey(challengeId));
            if (purpose.name().equals(state.get("purpose"))) {
                redis.execute(REVOKE, List.of(challengeKey(challengeId), activeKey(purpose, (String) state.get("uid"))), challengeId);
            }
            return true;
        });
    }

    public boolean acquireRateLimit(String scope, String subject, int limit, Duration window) {
        if (scope == null || subject == null || subject.isBlank() || subject.length() > 320 || limit < 1
                || window == null || window.toMillis() < 1) throw new IllegalArgumentException("Invalid rate limit input.");
        String key = base() + "rate:" + digests.digest("rate", scope, subject);
        return available(() -> success(redis.execute(RATE, List.of(key), Integer.toString(limit), millis(window))));
    }

    private String base() { return properties.keyPrefix() + ":{auth}:"; }
    private String challengeKey(String id) { return base() + "challenge:" + id; }
    private String activeKey(ChallengePurpose purpose, String userId) {
        return base() + "active:" + digests.digest("account", purpose.name(), userId);
    }
    private static boolean validId(String id) { return id != null && id.matches("[A-Za-z0-9_-]{43}"); }
    private static String millis(Duration duration) { return Long.toString(duration.toMillis()); }
    private static boolean success(Long result) {
        if (result == null) throw new TemporarySecurityUnavailableException();
        return result == 1L;
    }
    private static <T> T available(Supplier<T> action) {
        try { return action.get(); }
        catch (DataAccessException exception) { throw new TemporarySecurityUnavailableException(); }
    }
    private static DefaultRedisScript<Long> script(String name) {
        var script = new DefaultRedisScript<Long>();
        script.setLocation(new ClassPathResource("redis/" + name + ".lua"));
        script.setResultType(Long.class);
        return script;
    }
}
