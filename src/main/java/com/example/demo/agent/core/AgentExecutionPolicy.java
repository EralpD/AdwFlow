package com.example.demo.agent.core;

import java.util.Objects;
import java.util.function.Predicate;

public record AgentExecutionPolicy(
        int maximumAttempts,
        Predicate<Throwable> retryCondition
) {

    public AgentExecutionPolicy {
        if (maximumAttempts < 1) {
            throw new IllegalArgumentException(
                    "Maximum attempts must be greater than zero"
            );
        }

        Objects.requireNonNull(
                retryCondition,
                "Retry condition must not be null"
        );
    }

    public boolean shouldRetry(
            Throwable failure,
            int completedAttempts
    ) {
        return completedAttempts < maximumAttempts
                && retryCondition.test(failure);
    }

    public static AgentExecutionPolicy noRetry() {
        return new AgentExecutionPolicy(
                1,
                failure -> false
        );
    }

    public static AgentExecutionPolicy retry(
            int maximumAttempts,
            Predicate<Throwable> retryCondition
    ) {
        return new AgentExecutionPolicy(
                maximumAttempts,
                retryCondition
        );
    }
}