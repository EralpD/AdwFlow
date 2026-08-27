package com.example.demo.account;

import java.util.Locale;

public final class EmailAddresses {
    private EmailAddresses() {}

    public static String normalize(String email) {
        return email == null ? "" : email.strip().toLowerCase(Locale.ROOT);
    }
}
