package com.example.demo.security.temporary;

import java.time.Duration;

/** Server-only delivery envelope. Never return this object from an HTTP controller or log its secret. */
public record IssuedChallenge(ChallengePurpose purpose, String challengeId, String secret, Duration validFor) {
    @Override public String toString() { return "IssuedChallenge[purpose=" + purpose + ", credentials redacted]"; }
}
