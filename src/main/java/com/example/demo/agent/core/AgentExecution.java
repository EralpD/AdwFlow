package com.example.demo.agent.core;

import java.util.Objects;

public record AgentExecution<O>(
        O output,
        AgentExecutionMetadata metadata
) {

    public AgentExecution {
        Objects.requireNonNull(
                output,
                "Agent output must not be null"
        );

        Objects.requireNonNull(
                metadata,
                "Agent execution metadata must not be null"
        );
    }
}