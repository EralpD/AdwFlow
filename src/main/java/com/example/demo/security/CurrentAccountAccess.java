package com.example.demo.security;

import com.example.demo.account.UserAccountRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CurrentAccountAccess {
    private final UserAccountRepository accounts;

    public CurrentAccountAccess(UserAccountRepository accounts) {
        this.accounts = accounts;
    }

    @Transactional(readOnly = true)
    public boolean isAllowed(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof AccountPrincipal principal)
                || principal.getUserId() == null) {
            return false;
        }
        // The ID comes exclusively from the server-side login principal, never the request.
        // Recheck the database so deleted/recreated accounts cannot reuse an old session.
        return accounts.existsByIdAndEmailAndAuthVersion(principal.getUserId(), principal.getUsername(), principal.getAuthVersion());
    }
}
