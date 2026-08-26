package com.example.demo.generate.api;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.agent.core.AgentContext;
import com.example.demo.agent.core.AgentExecution;
import com.example.demo.agent.core.AgentExecutionPolicy;
import com.example.demo.agent.core.AgentExecutor;
import com.example.demo.agent.visual.VisualCreativeAgent;
import com.example.demo.agent.visual.VisualGenerationResult;

@RestController
@RequestMapping("/api/advertisements/visuals")
public final class GenerateAdvertisementVisualApiController {

    private final VisualCreativeAgent visualCreativeAgent;
    private final AgentExecutor agentExecutor;

    public GenerateAdvertisementVisualApiController(
        VisualCreativeAgent visualCreativeAgent,
        AgentExecutor agentExecutor
    ) {
        this.visualCreativeAgent = visualCreativeAgent;
        this.agentExecutor = agentExecutor;
    }

    @PostMapping("/generate")
    public GenerateAdvertisementVisualResponse generate(
        @RequestBody GenerateAdvertisementVisualRequest request
    ) {
        AgentContext context = AgentContext
            .initial(request.workflowId(), request.generationId())
            .withAttribute("stage", "visual-generation")
            .withAttribute("candidateId", request.candidateId());

        AgentExecution<VisualGenerationResult> execution =
            agentExecutor.execute(
                visualCreativeAgent,
                request.toAgentRequest(),
                context,
                AgentExecutionPolicy.noRetry()
            );

        return GenerateAdvertisementVisualResponse.from(
            execution.output()
        );
    }
}