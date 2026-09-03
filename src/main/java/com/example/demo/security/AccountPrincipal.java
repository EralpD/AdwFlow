package com.example.demo.security;

import com.example.demo.account.UserAccount;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import java.util.List;

public final class AccountPrincipal extends User {
    private static final long serialVersionUID = 1L;
    private final Long userId;
    private final String displayName;
    private final long authVersion;

    public AccountPrincipal(UserAccount account) {
        super(account.getEmail(), account.getPasswordHash(), account.getEmailVerifiedAt() != null,
                true, true, true, List.of(new SimpleGrantedAuthority("ROLE_" + account.getRole().name())));
        this.userId = account.getId();
        this.displayName = account.getDisplayName();
        this.authVersion = account.getAuthVersion();
    }

    public Long getUserId() { return userId; }
    public String getDisplayName() { return displayName; }
    public long getAuthVersion() { return authVersion; }
}
