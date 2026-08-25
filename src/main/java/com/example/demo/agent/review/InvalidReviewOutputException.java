package com.example.demo.agent.review;

// Specific type exception for:
/*
    - still returning 2 reviews for 3 candidates
    - changing candidate ID
    - unavailable confidence value
    - Emptiness of Finding field
    - Creating 2 same review for same candidate
 */

public final class InvalidReviewOutputException
        extends RuntimeException {

    public InvalidReviewOutputException(String message) {
        super(message);
    }

    public InvalidReviewOutputException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}