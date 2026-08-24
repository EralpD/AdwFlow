package com.example.demo.workflow;

// Strategy Agent Workflow

import com.example.demo.agent.core.AgentContext;
import com.example.demo.agent.core.AgentExecution;
import com.example.demo.agent.core.AgentExecutionPolicy;
import com.example.demo.agent.core.AgentExecutor;
import com.example.demo.agent.strategy.CreativeStrategistAgent;
import com.example.demo.agent.strategy.StrategyRequest;
import com.example.demo.agent.strategy.StrategyResult;
import org.springframework.stereotype.Service;

@Service
public final class AdvertisingWorkflow {

    private final CreativeStrategistAgent strategistAgent;
    private final AgentExecutor agentExecutor;

    public AdvertisingWorkflow(
            CreativeStrategistAgent strategistAgent,
            AgentExecutor agentExecutor
    ) {
        this.strategistAgent = strategistAgent;
        this.agentExecutor = agentExecutor;
    }

    public StrategyResult createStrategy(
            StrategyRequest request,
            AgentContext context
    ) {
        AgentExecution<StrategyResult> execution =
                agentExecutor.execute(
                        strategistAgent,
                        request,
                        context,
                        AgentExecutionPolicy.noRetry()
                );

        return execution.output();
    }
}