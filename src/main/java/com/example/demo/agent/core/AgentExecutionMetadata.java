package com.example.demo.agent.core;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public record AgentExecutionMetadata(
        AgentDescriptor agent,
        String workflowId,
        String generationId,
        int attempts,
        Instant startedAt,
        Instant completedAt
) {

    public AgentExecutionMetadata {
        Objects.requireNonNull(agent, "Agent must not be null");
        Objects.requireNonNull(startedAt, "Started time must not be null");
        Objects.requireNonNull(completedAt, "Completed time must not be null");

        if (workflowId == null || workflowId.isBlank()) {
            throw new IllegalArgumentException(
                    "Workflow ID must not be blank"
            );
        }

        if (generationId == null || generationId.isBlank()) {
            throw new IllegalArgumentException(
                    "Generation ID must not be blank"
            );
        }

        if (attempts < 1) {
            throw new IllegalArgumentException(
                    "Attempts must be greater than zero"
            );
        }

        if (completedAt.isBefore(startedAt)) {
            throw new IllegalArgumentException(
                    "Completed time must not be before started time"
            );
        }
    }

    public Duration duration() {
        return Duration.between(startedAt, completedAt);
    }
}