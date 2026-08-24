package com.example.demo.agent.core;

// Runs the whole agent/core in this file

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

public final class AgentExecutor {

    private final AgentExecutionObserver observer;
    private final Clock clock;

    public AgentExecutor(
            AgentExecutionObserver observer,
            Clock clock
    ) {
        this.observer = Objects.requireNonNull(observer);
        this.clock = Objects.requireNonNull(clock);
    }

    public <I, O> AgentExecution<O> execute(
            Agent<I, O> agent,
            I input,
            AgentContext context,
            AgentExecutionPolicy policy
    ) {
        Objects.requireNonNull(agent);
        Objects.requireNonNull(input);
        Objects.requireNonNull(context);
        Objects.requireNonNull(policy);

        Instant executionStartedAt = clock.instant();
        RuntimeException lastFailure = null;

        for (
                int attempt = 1;
                attempt <= policy.maximumAttempts();
                attempt++
        ) {
            AgentContext attemptContext =
                    context.withAttempt(attempt);

            AgentExecutionObserver.AgentObservation observation =
                    observer.begin(
                            agent.descriptor(),
                            attemptContext
                    );

            try {
                O output = agent.execute(
                        input,
                        attemptContext
                );

                if (output == null) {
                    throw new IllegalStateException(
                            "Agent returned a null output: "
                                    + agent.descriptor().name()
                    );
                }

                Instant completedAt = clock.instant();

                AgentExecutionMetadata metadata =
                        new AgentExecutionMetadata(
                                agent.descriptor(),
                                context.workflowId(),
                                context.generationId(),
                                attempt,
                                executionStartedAt,
                                completedAt
                        );

                observation.success(metadata);

                return new AgentExecution<>(
                        output,
                        metadata
                );
            } catch (RuntimeException failure) {
                lastFailure = failure;
                observation.failure(failure);

                if (!policy.shouldRetry(failure, attempt)) {
                    throw new AgentExecutionException(
                            agent.descriptor(),
                            attemptContext,
                            attempt,
                            failure
                    );
                }
            } finally {
                observation.close();
            }
        }

        throw new AgentExecutionException(
                agent.descriptor(),
                context,
                policy.maximumAttempts(),
                lastFailure
        );
    }
}