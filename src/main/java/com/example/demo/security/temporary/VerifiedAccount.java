package com.example.demo.security.temporary;

/** Only returned after successful atomic consumption; still requires a conditional SQL update. */
public record VerifiedAccount(long userId, long authVersion) {}
