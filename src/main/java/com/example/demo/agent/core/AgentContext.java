package com.example.demo.agent.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public record AgentContext(
        String workflowId,
        String generationId,
        int attempt,
        Map<String, Object> attributes
) {

    public AgentContext {
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

        if (attempt < 1) {
            throw new IllegalArgumentException(
                    "Attempt must be greater than zero"
            );
        }

        attributes = attributes == null
                ? Map.of()
                : Map.copyOf(attributes);
    }

    public static AgentContext initial(
            String workflowId,
            String generationId
    ) {
        return new AgentContext(
                workflowId,
                generationId,
                1,
                Map.of()
        );
    }

    public AgentContext withAttempt(int newAttempt) {
        return new AgentContext(
                workflowId,
                generationId,
                newAttempt,
                attributes
        );
    }

    public AgentContext withAttribute(
            String key,
            Object value
    ) {
        Objects.requireNonNull(key, "Attribute key must not be null");
        Objects.requireNonNull(value, "Attribute value must not be null");

        Map<String, Object> updated =
                new HashMap<>(attributes);

        updated.put(key, value);

        return new AgentContext(
                workflowId,
                generationId,
                attempt,
                updated
        );
    }
}