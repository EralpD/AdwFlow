package com.example.demo.agent.core;

// Prevent the dependency through to Telemetry

@FunctionalInterface
public interface AgentExecutionObserver {

    AgentObservation begin(
            AgentDescriptor descriptor,
            AgentContext context
    );

    interface AgentObservation extends AutoCloseable {

        default void success(
                AgentExecutionMetadata metadata
        ) {
        }

        default void failure(Throwable failure) {
        }

        @Override
        default void close() {
        }
    }

    static AgentExecutionObserver noop() {
        return (descriptor, context) ->
                new AgentObservation() {
                };
    }
}