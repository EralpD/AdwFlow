package com.example.demo.account;

public final class DuplicateEmailException extends RuntimeException {
    public DuplicateEmailException() {
        super("An account could not be created with those details.");
    }
}
