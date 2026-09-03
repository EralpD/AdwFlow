package com.example.demo.account;

import com.example.demo.security.AccountPrincipal;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AccountVerificationStateTest {
    @Test
    void normalRegistrationsCannotAuthenticateBeforeEmailVerification() {
        var account = new UserAccount("Ada", "ada@example.com", "{bcrypt}hash", Role.USER);
        assertThat(new AccountPrincipal(account).isEnabled()).isFalse();
    }

    @Test
    void bootstrapAdminsRemainUsableWithoutAnInteractiveEmailFlow() {
        var account = new UserAccount("Admin", "admin@example.com", "{bcrypt}hash", Role.ADMIN);
        account.markEmailVerifiedForBootstrap();
        assertThat(new AccountPrincipal(account).isEnabled()).isTrue();
    }
}
