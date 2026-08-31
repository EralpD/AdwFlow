package com.example.demo.agent.strategy.internal;

// Management of requesting/regulating STs, creating messages of System and User

import com.example.demo.agent.strategy.StrategyRequest;
import com.example.demo.workflow.context.TrustedContextPromptFormatter;
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
    private final TrustedContextPromptFormatter trustedContextFormatter;

    public StrategyPromptFactory(
            @Value("classpath:prompts/strategy/system.st")
            Resource systemPromptResource,

            @Value("classpath:prompts/strategy/create-strategy.st")
            Resource createStrategyPromptResource,

            TemplateRenderer templateRenderer,
            TrustedContextPromptFormatter trustedContextFormatter
    ) {
        this.systemPromptResource = systemPromptResource;
        this.createStrategyPromptResource =
                createStrategyPromptResource;
        this.templateRenderer = templateRenderer;
        this.trustedContextFormatter = trustedContextFormatter;
    }

    public Message createSystemMessage() {
        return SystemPromptTemplate.builder()
                .resource(systemPromptResource)
                .renderer(templateRenderer)
                .build()
                .createMessage();
    }

    public Message createUserMessage(StrategyRequest request) {
        Map<String, Object> variables = Map.ofEntries(
                Map.entry("brief", request.brief()),
                Map.entry("platform", request.platform()),
                Map.entry("brandName", request.brandName()),
                Map.entry("brandVoice", request.brandVoice()),
                Map.entry("knownTargetAudience", request.knownTargetAudience()),
                Map.entry("language", request.language()),
                Map.entry("requestedAngleCount", request.requestedAngleCount()),
                Map.entry("trustedContext", trustedContextFormatter.format(request.trustedContext())),
                Map.entry("revisionGuidance", request.revisionGuidance().isEmpty()
                        ? "None — this is the initial strategy."
                        : String.join("\n- ", request.revisionGuidance()))
        );

        return PromptTemplate.builder()
                .resource(createStrategyPromptResource)
                .renderer(templateRenderer)
                .variables(variables)
                .build()
                .createMessage();
    }
}
