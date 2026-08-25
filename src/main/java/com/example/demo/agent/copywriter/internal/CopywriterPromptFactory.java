package com.example.demo.agent.copywriter.internal;

// Essential prompt functions belong in here

import java.util.Map;

import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.template.TemplateRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import com.example.demo.agent.copywriter.CopywriterRequest;
import com.example.demo.agent.copywriter.GenerateCopyRequest;
import com.example.demo.agent.copywriter.ReviseCopyRequest;

@Component
public final class CopywriterPromptFactory {

    private final Resource systemPromptResource;
    private final Resource generationPromptResource;
    private final Resource revisionPromptResource;
    private final TemplateRenderer templateRenderer;
    private final CopywriterPromptFormatter formatter;

    public CopywriterPromptFactory(
            @Value("classpath:prompts/copywriter/system.st")
            Resource systemPromptResource,

            @Value(
                    "classpath:prompts/copywriter/"
                            + "generate-candidate.st"
            )
            Resource generationPromptResource,

            @Value(
                    "classpath:prompts/copywriter/"
                            + "revise-candidate.st"
            )
            Resource revisionPromptResource,

            TemplateRenderer templateRenderer,
            CopywriterPromptFormatter formatter
    ) {
        this.systemPromptResource = systemPromptResource;
        this.generationPromptResource = generationPromptResource;
        this.revisionPromptResource = revisionPromptResource;
        this.templateRenderer = templateRenderer;
        this.formatter = formatter;
    }

    public Message createSystemMessage() {
        return SystemPromptTemplate.builder()
                .resource(systemPromptResource)
                .renderer(templateRenderer)
                .build()
                .createMessage();
    }

    public Message createUserMessage(
            CopywriterRequest request
    ) {
        return switch (request) {
            case GenerateCopyRequest generateRequest ->
                    createGenerationMessage(generateRequest);

            case ReviseCopyRequest reviseRequest ->
                    createRevisionMessage(reviseRequest);
        };
    }

    private Message createGenerationMessage(
            GenerateCopyRequest request
    ) {
        Map<String, Object> variables = Map.of(
                "platform",
                request.platform(),
                "language",
                request.language(),
                "expectedCandidateCount",
                request.strategy().creativeAngles().size(),
                "strategy",
                formatter.formatStrategy(request.strategy())
        );

        return PromptTemplate.builder()
                .resource(generationPromptResource)
                .renderer(templateRenderer)
                .variables(variables)
                .build()
                .createMessage();
    }

    private Message createRevisionMessage(
            ReviseCopyRequest request
    ) {
        Map<String, Object> variables = Map.of(
                "platform",
                request.platform(),
                "language",
                request.language(),
                "expectedCandidateCount",
                request.candidatesToRevise().size(),
                "strategy",
                formatter.formatStrategy(request.strategy()),
                "candidates",
                formatter.formatCandidates(
                        request.candidatesToRevise()
                ),
                "revisionInstructions",
                formatter.formatRevisionInstructions(
                        request.revisionInstructions()
                )
        );

        return PromptTemplate.builder()
                .resource(revisionPromptResource)
                .renderer(templateRenderer)
                .variables(variables)
                .build()
                .createMessage();
    }
}