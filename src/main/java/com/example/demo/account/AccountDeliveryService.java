package com.example.demo.account;

import com.example.demo.account.mail.AccountMailGateway;
import org.springframework.stereotype.Service;

@Service
public class AccountDeliveryService {
    private final AccountRecoveryService recovery;
    private final AccountMailGateway mail;

    public AccountDeliveryService(AccountRecoveryService recovery, AccountMailGateway mail) {
        this.recovery = recovery;
        this.mail = mail;
    }

    public DispatchResult sendEmailVerification(String email, String clientAddress) {
        return recovery.requestEmailVerification(email, clientAddress)
                .map(delivery -> new DispatchResult(delivery.challenge().challengeId(), mail.deliver(delivery)))
                .orElseGet(DispatchResult::notIssued);
    }

    /** The caller must always return the same public response, regardless of this result. */
    public void sendPasswordReset(String email, String clientAddress) {
        recovery.requestPasswordReset(email, clientAddress).ifPresent(mail::deliver);
    }

    public boolean resetPassword(String challengeId, String token, String password, String confirmation,
            String clientAddress) {
        return recovery.resetPasswordAndGetEmail(challengeId, token, password, confirmation, clientAddress)
                .map(recipient -> {
                    // Notification failure must never roll back or misreport the completed password change.
                    mail.notifyPasswordChanged(recipient);
                    return true;
                })
                .orElse(false);
    }

    public record DispatchResult(String challengeId, boolean deliveryAccepted) {
        public boolean issued() { return challengeId != null; }
        private static DispatchResult notIssued() { return new DispatchResult(null, false); }
    }
}
