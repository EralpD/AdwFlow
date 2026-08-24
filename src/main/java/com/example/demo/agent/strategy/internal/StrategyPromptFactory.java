package com.example.demo.agent.strategy.internal;

// Management of requesting/regulating STs, creating messages of System and User

import com.example.demo.agent.strategy.StrategyRequest;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.template.TemplateRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public final class StrategyPromptFactory {

    private final Resource systemPromptResource;
    private final Resource createStrategyPromptResource;
    private final TemplateRenderer templateRenderer;

    public StrategyPromptFactory(
            @Value("classpath:prompts/strategy/system.st")
            Resource systemPromptResource,

            @Value("classpath:prompts/strategy/create-strategy.st")
            Resource createStrategyPromptResource,

            TemplateRenderer templateRenderer
    ) {
        this.systemPromptResource = systemPromptResource;
        this.createStrategyPromptResource =
                createStrategyPromptResource;
        this.templateRenderer = templateRenderer;
    }

    public Message createSystemMessage() {
        return SystemPromptTemplate.builder()
                .resource(systemPromptResource)
                .renderer(templateRenderer)
                .build()
                .createMessage();
    }

    public Message createUserMessage(StrategyRequest request) {
        Map<String, Object> variables = Map.of(
                "brief", request.brief(),
                "platform", request.platform(),
                "brandName", request.brandName(),
                "brandVoice", request.brandVoice(),
                "knownTargetAudience",
                request.knownTargetAudience(),
                "requestedAngleCount",
                request.requestedAngleCount()
        );

        return PromptTemplate.builder()
                .resource(createStrategyPromptResource)
                .renderer(templateRenderer)
                .variables(variables)
                .build()
                .createMessage();
    }
}