package com.example.demo.security;

import com.example.demo.account.EmailAddresses;
import com.example.demo.account.UserAccountRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountDetailsService implements UserDetailsService {
    private final UserAccountRepository accounts;

    public AccountDetailsService(UserAccountRepository accounts) {
        this.accounts = accounts;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) {
        return accounts.findByEmail(EmailAddresses.normalize(email))
                .map(AccountPrincipal::new)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid email or password."));
    }
}
