package com.example.demo.agent.core;

import java.util.Objects;

public final class AgentExecutionException
        extends RuntimeException {

    private final AgentDescriptor agent;
    private final String workflowId;
    private final String generationId;
    private final int attempts;

    public AgentExecutionException(
            AgentDescriptor agent,
            AgentContext context,
            int attempts,
            Throwable cause
    ) {
        super(
                "Agent '%s' failed after %d attempt(s)"
                        .formatted(agent.name(), attempts),
                cause
        );

        this.agent = Objects.requireNonNull(agent);
        this.workflowId = context.workflowId();
        this.generationId = context.generationId();
        this.attempts = attempts;
    }

    public AgentDescriptor agent() {
        return agent;
    }

    public String workflowId() {
        return workflowId;
    }

    public String generationId() {
        return generationId;
    }

    public int attempts() {
        return attempts;
    }
}