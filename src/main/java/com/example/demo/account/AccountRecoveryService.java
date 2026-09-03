package com.example.demo.account;

import com.example.demo.security.temporary.*;
import jakarta.validation.Validator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.Instant;
import java.util.Optional;

/** Backend integration boundary for account challenge issuance and consumption; not an HTTP API. */
@Service
public class AccountRecoveryService {
    private final UserAccountRepository accounts;
    private final RedisChallengeStore challenges;
    private final RedisSecurityProperties properties;
    private final PasswordEncoder passwords;
    private final Validator validator;
    private final TransactionTemplate transaction;

    public AccountRecoveryService(UserAccountRepository accounts, RedisChallengeStore challenges,
            RedisSecurityProperties properties, PasswordEncoder passwords, Validator validator,
            PlatformTransactionManager transactions) {
        this.accounts = accounts;
        this.challenges = challenges;
        this.properties = properties;
        this.passwords = passwords;
        this.validator = validator;
        this.transaction = new TransactionTemplate(transactions);
    }

    public Optional<Delivery> requestEmailVerification(String email, String clientAddress) {
        return request(ChallengePurpose.EMAIL_VERIFICATION, email, clientAddress);
    }

    public Optional<Delivery> requestPasswordReset(String email, String clientAddress) {
        return request(ChallengePurpose.PASSWORD_RESET, email, clientAddress);
    }

    private Optional<Delivery> request(ChallengePurpose purpose, String email, String clientAddress) {
        String normalized = EmailAddresses.normalize(email);
        RegistrationForm input = new RegistrationForm();
        input.setEmail(normalized);
        if (!validator.validateProperty(input, "email").isEmpty()) throw new IllegalArgumentException("Invalid email address.");
        checkAddress(clientAddress);
        // Both buckets run before the account lookup; unknown addresses are throttled too.
        if (!challenges.acquireRateLimit(purpose + ":request:ip", clientAddress, properties.ipRequestLimit(), properties.requestWindow())
                || !challenges.acquireRateLimit(purpose + ":request:email", normalized, properties.emailRequestLimit(), properties.requestWindow())) {
            return Optional.empty();
        }
        return accounts.findByEmail(normalized)
                .filter(account -> purpose != ChallengePurpose.EMAIL_VERIFICATION || account.getEmailVerifiedAt() == null)
                .flatMap(account -> challenges.issue(purpose, account.getId(), account.getAuthVersion())
                        .map(challenge -> new Delivery(account.getEmail(), challenge)));
    }

    public boolean verifyEmail(String challengeId, String code, String clientAddress) {
        if (!allowConfirmation(ChallengePurpose.EMAIL_VERIFICATION, clientAddress)) return false;
        return challenges.consume(ChallengePurpose.EMAIL_VERIFICATION, challengeId, code)
                .map(proof -> Boolean.TRUE.equals(transaction.execute(status ->
                        accounts.markEmailVerified(proof.userId(), proof.authVersion(), Instant.now()) == 1)))
                .orElse(false);
    }

    public boolean resetPassword(String challengeId, String token, String password, String confirmation, String clientAddress) {
        return resetPasswordAndGetEmail(challengeId, token, password, confirmation, clientAddress).isPresent();
    }

    /** Returns the recipient only to the trusted delivery coordinator after a successful password change. */
    public Optional<String> resetPasswordAndGetEmail(String challengeId, String token, String password,
            String confirmation, String clientAddress) {
        // Reuse the registration policy, including BCrypt's byte limit and confirmation rule.
        RegistrationForm input = new RegistrationForm();
        input.setDisplayName("Recovery");
        input.setEmail("recovery@example.invalid");
        input.setPassword(password);
        input.setConfirmPassword(confirmation);
        if (!validator.validate(input).isEmpty()) throw new IllegalArgumentException("Invalid password or password confirmation.");
        if (!allowConfirmation(ChallengePurpose.PASSWORD_RESET, clientAddress)) return Optional.empty();
        var proof = challenges.consume(ChallengePurpose.PASSWORD_RESET, challengeId, token);
        if (proof.isEmpty()) return Optional.empty();
        String recipient = accounts.findEmailById(proof.get().userId()).orElse(null);
        if (recipient == null) return Optional.empty();
        String hash = passwords.encode(password);
        // Consume before SQL. On SQL failure the token stays consumed; request a new email.
        // The version predicate also rejects stale credentials after Redis backup restoration.
        boolean changed = Boolean.TRUE.equals(transaction.execute(status -> accounts.resetPassword(
                proof.get().userId(), proof.get().authVersion(), hash) == 1));
        return changed ? Optional.of(recipient) : Optional.empty();
    }

    private boolean allowConfirmation(ChallengePurpose purpose, String address) {
        checkAddress(address);
        return challenges.acquireRateLimit(purpose + ":confirm:ip", address, properties.ipConfirmLimit(), properties.confirmWindow());
    }

    private static void checkAddress(String address) {
        if (address == null || address.isBlank() || address.length() > 128) {
            throw new IllegalArgumentException("A server-resolved client address is required.");
        }
    }

    /** Never serialize to the browser; pass only to a trusted mail delivery adapter. */
    public record Delivery(String email, IssuedChallenge challenge) {
        @Override public String toString() { return "Delivery[recipient and credentials redacted]"; }
    }
}
