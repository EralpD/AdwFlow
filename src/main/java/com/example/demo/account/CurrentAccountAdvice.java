package com.example.demo.account;

import com.example.demo.security.AccountLoginSuccessHandler;
import com.example.demo.security.AccountPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(annotations = org.springframework.stereotype.Controller.class)
public class CurrentAccountAdvice {
    // Only this safe projection is exposed to views; never an entity or credential-bearing principal.
    public record CurrentAccount(String displayName, String email, boolean admin) {}

    @ModelAttribute("currentUser")
    public CurrentAccount currentAccount(Authentication authentication) {
        if (authentication == null) return null;
        String name = authentication.getPrincipal() instanceof AccountPrincipal account
                ? account.getDisplayName() : authentication.getName();
        return new CurrentAccount(name, authentication.getName(), AccountLoginSuccessHandler.isAdmin(authentication));
    }
}
