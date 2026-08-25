package com.example.demo.telemetry;

import com.example.demo.agent.core.AgentContext;
import com.example.demo.agent.core.AgentDescriptor;
import com.example.demo.agent.core.AgentExecutionMetadata;
import com.example.demo.agent.core.AgentExecutionObserver;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MicrometerAgentExecutionObserverTests {

    @Test
    void recordsSuccessfulAgentSpanAndMetrics() {
        Tracer tracer = mock(Tracer.class);
        Span span = mock(Span.class);
        Tracer.SpanInScope scope =
                mock(Tracer.SpanInScope.class);
        SimpleMeterRegistry meterRegistry =
                new SimpleMeterRegistry();

        when(tracer.nextSpan()).thenReturn(span);
        when(span.name(anyString())).thenReturn(span);
        when(span.tag(anyString(), anyString()))
                .thenReturn(span);
        when(span.start()).thenReturn(span);
        when(tracer.withSpan(span)).thenReturn(scope);

        MicrometerAgentExecutionObserver observer =
                new MicrometerAgentExecutionObserver(
                        tracer,
                        meterRegistry
                );

        AgentDescriptor descriptor = AgentDescriptor.of(
                "copywriter",
                "1.0.0",
                "candidate-generation"
        );

        AgentContext context = AgentContext.initial(
                "workflow-1",
                "generation-1"
        ).withAttribute("stage", "copywriter");

        AgentExecutionObserver.AgentObservation observation =
                observer.begin(descriptor, context);

        observation.success(
                new AgentExecutionMetadata(
                        descriptor,
                        "workflow-1",
                        "generation-1",
                        1,
                        Instant.parse("2026-01-01T00:00:00Z"),
                        Instant.parse("2026-01-01T00:00:01Z")
                )
        );
        observation.close();

        assertEquals(
                1.0,
                meterRegistry.get("agent.executions")
                        .tag("agent", "copywriter")
                        .tag("stage", "copywriter")
                        .tag("status", "success")
                        .counter()
                        .count()
        );

        assertEquals(
                1L,
                meterRegistry.get("agent.execution.duration")
                        .tag("agent", "copywriter")
                        .tag("stage", "copywriter")
                        .tag("status", "success")
                        .timer()
                        .count()
        );

        verify(scope).close();
        verify(span).end();
    }
}
