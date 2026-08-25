package com.example.demo.agent.copywriter;

// Specific type exception for:
/*
    - Not turning candidates from at least one of angle
    - Same candidate ID turns from two candidates
    - Any of strategy angle has been skipped
    - Emptiness of headline or primary text fields
    - Changing ID when in revision phase
 */

public final class InvalidCopywriterOutputException
        extends RuntimeException {

    public InvalidCopywriterOutputException(String message) {
        super(message);
    }

    public InvalidCopywriterOutputException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}