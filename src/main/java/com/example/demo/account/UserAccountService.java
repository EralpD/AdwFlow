package com.example.demo.account;

import jakarta.validation.Validator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserAccountService {
    private final UserAccountRepository accounts;
    private final PasswordEncoder passwords;
    private final Validator validator;

    public UserAccountService(UserAccountRepository accounts, PasswordEncoder passwords, Validator validator) {
        this.accounts = accounts;
        this.passwords = passwords;
        this.validator = validator;
    }

    @Transactional
    public UserAccount register(RegistrationForm form) {
        validate(form);
        if (accounts.existsByEmail(form.getEmail())) {
            throw new DuplicateEmailException();
        }
        // The database unique constraint is the final arbiter for concurrent registrations.
        return accounts.saveAndFlush(new UserAccount(form.getDisplayName(), form.getEmail(),
                passwords.encode(form.getPassword()), Role.USER));
    }

    @Transactional
    public void bootstrapAdmin(String name, String email, String password) {
        RegistrationForm form = new RegistrationForm();
        form.setDisplayName(name);
        form.setEmail(email);
        form.setPassword(password);
        form.setConfirmPassword(password);
        validate(form);

        var existing = accounts.findByEmail(form.getEmail());
        if (existing.isPresent()) {
            if (existing.get().getRole() != Role.ADMIN) {
                throw new IllegalStateException("Admin bootstrap refuses to promote an existing non-admin account.");
            }
            return; // An existing admin's name, role and password remain untouched.
        }
        var admin = new UserAccount(form.getDisplayName(), form.getEmail(), passwords.encode(password), Role.ADMIN);
        admin.markEmailVerifiedForBootstrap();
        accounts.saveAndFlush(admin);
    }

    private void validate(RegistrationForm form) {
        if (form == null || !validator.validate(form).isEmpty()) {
            throw new IllegalArgumentException("Invalid account details. Check the documented account requirements.");
        }
    }
}
