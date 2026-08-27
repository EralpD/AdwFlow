package com.example.demo.account;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.admin-bootstrap.enabled", havingValue = "true")
public class AdminBootstrap implements ApplicationRunner {
    private final UserAccountService accounts;
    private final String name;
    private final String email;
    private String password;

    public AdminBootstrap(UserAccountService accounts,
            @Value("${app.admin-bootstrap.name:}") String name,
            @Value("${app.admin-bootstrap.email:}") String email,
            @Value("${app.admin-bootstrap.password:}") String password) {
        this.accounts = accounts;
        this.name = name;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            accounts.bootstrapAdmin(name, email, password);
        } finally {
            password = null;
        }
    }
}
