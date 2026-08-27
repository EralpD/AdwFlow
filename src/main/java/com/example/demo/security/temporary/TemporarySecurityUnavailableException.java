package com.example.demo.security.temporary;

public class TemporarySecurityUnavailableException extends RuntimeException {
    public TemporarySecurityUnavailableException() {
        // Do not retain connection exceptions: they can include credential-bearing Redis URIs.
        super("Temporary security storage is unavailable. Try again later.");
    }
}
