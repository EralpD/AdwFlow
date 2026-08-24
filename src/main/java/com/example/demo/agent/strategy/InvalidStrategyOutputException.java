package com.example.demo.agent.strategy;

// Controllable exception

public final class InvalidStrategyOutputException
        extends RuntimeException {

    public InvalidStrategyOutputException(String message) {
        super(message);
    }

    public InvalidStrategyOutputException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}