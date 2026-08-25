package com.example.demo.agent.review;

// Enum for finding severity in review

public enum FindingSeverity {

    WARNING(false),
    ERROR(true),
    CRITICAL(true);

    private final boolean requiresRevision;

    FindingSeverity(boolean requiresRevision) {
        this.requiresRevision = requiresRevision;
    }

    public boolean requiresRevision() {
        return requiresRevision;
    }
}