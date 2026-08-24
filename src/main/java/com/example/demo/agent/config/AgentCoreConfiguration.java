package com.example.demo.agent.config;

// Classic configuration with using Beans

import java.time.Clock;

import com.example.demo.agent.core.AgentExecutionObserver;
import com.example.demo.agent.core.AgentExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AgentCoreConfiguration {

    @Bean
    Clock agentClock() {
        return Clock.systemUTC();
    }

    @Bean
    AgentExecutionObserver agentExecutionObserver() {
        return AgentExecutionObserver.noop();
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