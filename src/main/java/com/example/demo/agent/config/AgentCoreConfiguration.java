package com.example.demo.agent.config;

// Classic configuration with using Beans

import java.time.Clock;

import com.example.demo.agent.core.AgentExecutionObserver;
import com.example.demo.agent.core.AgentExecutor;
import com.example.demo.telemetry.MicrometerAgentExecutionObserver;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.tracing.Tracer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AgentCoreConfiguration {

    @Bean
    Clock agentClock() {
        return Clock.systemUTC();
    }

    @Bean
    AgentExecutionObserver agentExecutionObserver(
            ObjectProvider<Tracer> tracerProvider,
            MeterRegistry meterRegistry
    ) {
        Tracer tracer = tracerProvider.getIfAvailable();

        if (tracer == null) {
            return AgentExecutionObserver.noop();
        }

        return new MicrometerAgentExecutionObserver(
                tracer,
                meterRegistry
        );
    }

    @Bean
    AgentExecutor agentExecutor(
            AgentExecutionObserver observer,
            Clock agentClock
    ) {
        return new AgentExecutor(
                observer,
                agentClock
        );
    }
}
