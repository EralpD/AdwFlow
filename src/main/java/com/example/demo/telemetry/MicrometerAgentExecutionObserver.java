package com.example.demo.telemetry;

import com.example.demo.agent.core.AgentContext;
import com.example.demo.agent.core.AgentDescriptor;
import com.example.demo.agent.core.AgentExecutionMetadata;
import com.example.demo.agent.core.AgentExecutionObserver;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;

import java.time.Duration;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public final class MicrometerAgentExecutionObserver
        implements AgentExecutionObserver {

    private static final String EXECUTION_COUNTER =
            "agent.executions";
    private static final String DURATION_TIMER =
            "agent.execution.duration";
    private static final String AD_GENERATION_TRACE =
            "advertisement-generation";
    private static final String VISUAL_GENERATION_TRACE =
            "advertisement-visual-generation";

    private final Tracer tracer;
    private final MeterRegistry meterRegistry;

    public MicrometerAgentExecutionObserver(
            Tracer tracer,
            MeterRegistry meterRegistry
    ) {
        this.tracer = tracer;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public AgentObservation begin(
            AgentDescriptor descriptor,
            AgentContext context
    ) {
        String stage = stage(context);
        String traceName = traceName(stage);

        Span span = tracer.nextSpan()
                .name("agent." + descriptor.name())
                .tag("agent.name", descriptor.name())
                .tag("agent.version", descriptor.version())
                .tag("agent.stage", stage)
                .tag("agent.attempt", Integer.toString(
                        context.attempt()
                ))
                .tag("workflow.id", context.workflowId())
                .tag("generation.id", context.generationId())
                .tag("langfuse.trace.name", traceName)
                .tag(
                        "langfuse.session.id",
                        context.workflowId()
                )
                .tag(
                        "langfuse.trace.metadata.workflow_id",
                        context.workflowId()
                )
                .tag(
                        "langfuse.trace.metadata.generation_id",
                        context.generationId()
                )
                .tag("langfuse.observation.type", "span")
                .tag(
                        "langfuse.observation.metadata.agent_name",
                        descriptor.name()
                )
                .tag(
                        "langfuse.observation.metadata.agent_version",
                        descriptor.version()
                )
                .tag(
                        "langfuse.observation.metadata.stage",
                        stage
                )
                .tag(
                        "agent.capabilities",
                        capabilities(descriptor)
                )
                .start();

        Tracer.SpanInScope scope = tracer.withSpan(span);

        return new MicrometerAgentObservation(
                descriptor,
                stage,
                span,
                scope,
                System.nanoTime()
        );
    }

    private String stage(AgentContext context) {
        Object value = context.attributes().get("stage");

        if (value == null) {
            return "unspecified";
        }

        String stage = value.toString().trim();
        return stage.isEmpty() ? "unspecified" : stage;
    }

    private String traceName(String stage) {
        if ("visual-generation".equals(stage)) {
            return VISUAL_GENERATION_TRACE;
        }

        return AD_GENERATION_TRACE;
    }

    private String capabilities(AgentDescriptor descriptor) {
        return descriptor.capabilities().stream()
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.joining(","));
    }

    private final class MicrometerAgentObservation
            implements AgentObservation {

        private final AgentDescriptor descriptor;
        private final String stage;
        private final Span span;
        private final Tracer.SpanInScope scope;
        private final long startedAtNanos;
        private final AtomicBoolean outcomeRecorded =
                new AtomicBoolean();
        private final AtomicBoolean closed =
                new AtomicBoolean();

        private MicrometerAgentObservation(
                AgentDescriptor descriptor,
                String stage,
                Span span,
                Tracer.SpanInScope scope,
                long startedAtNanos
        ) {
            this.descriptor = descriptor;
            this.stage = stage;
            this.span = span;
            this.scope = scope;
            this.startedAtNanos = startedAtNanos;
        }

        @Override
        public void success(
                AgentExecutionMetadata metadata
        ) {
            if (!outcomeRecorded.compareAndSet(false, true)) {
                return;
            }

            span.tag("agent.status", "success");
            span.tag(
                    "langfuse.observation.level",
                    "DEFAULT"
            );
            span.tag(
                    "agent.total_attempts",
                    Integer.toString(metadata.attempts())
            );
            span.event("agent.completed");
            recordMetrics("success");
        }

        @Override
        public void failure(Throwable failure) {
            if (!outcomeRecorded.compareAndSet(false, true)) {
                return;
            }

            span.tag("agent.status", "error");
            span.tag(
                    "langfuse.observation.level",
                    "ERROR"
            );
            span.tag(
                    "langfuse.observation.status_message",
                    failure.getClass().getSimpleName()
            );
            span.tag(
                    "error.type",
                    failure.getClass().getSimpleName()
            );
            span.error(failure);
            span.event("agent.failed");
            recordMetrics("error");
        }

        @Override
        public void close() {
            if (!closed.compareAndSet(false, true)) {
                return;
            }

            if (outcomeRecorded.compareAndSet(false, true)) {
                span.tag("agent.status", "unknown");
                recordMetrics("unknown");
            }

            scope.close();
            span.end();
        }

        private void recordMetrics(String status) {
            Duration duration = Duration.ofNanos(
                    System.nanoTime() - startedAtNanos
            );

            Counter.builder(EXECUTION_COUNTER)
                    .description("Number of agent execution attempts")
                    .tag("agent", descriptor.name())
                    .tag("stage", stage)
                    .tag("status", status)
                    .register(meterRegistry)
                    .increment();

            Timer.builder(DURATION_TIMER)
                    .description("Agent execution attempt duration")
                    .tag("agent", descriptor.name())
                    .tag("stage", stage)
                    .tag("status", status)
                    .register(meterRegistry)
                    .record(duration);

            span.tag(
                    "agent.duration.ms",
                    Long.toString(duration.toMillis())
            );
        }
    }
}
