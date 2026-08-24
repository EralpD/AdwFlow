package com.example.demo.agent.core;

import java.util.Set;

public record AgentDescriptor(
        String name,
        String version,
        Set<String> capabilities
) {

    public AgentDescriptor {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Agent name must not be blank"
            );
        }

        if (version == null || version.isBlank()) {
            throw new IllegalArgumentException(
                    "Agent version must not be blank"
            );
        }

        capabilities = capabilities == null
                ? Set.of()
                : Set.copyOf(capabilities);
    }

    public static AgentDescriptor of(
            String name,
            String version,
            String... capabilities
    ) {
        return new AgentDescriptor(
                name,
                version,
                Set.of(capabilities)
        );
    }
}