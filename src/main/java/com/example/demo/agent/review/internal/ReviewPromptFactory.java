package com.example.demo.agent.review.internal;

// Only work that Prompt factory does:
/*
    - Loading .st files
    - Declaring Strategy and candidate data
    - Creating messages for system and user
 */

import java.util.Map;

import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.template.TemplateRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import com.example.demo.agent.review.ReviewRequest;

@Component
public final class ReviewPromptFactory {

    private final Resource systemPromptResource;
    private final Resource reviewPromptResource;
    private final TemplateRenderer templateRenderer;
    private final ReviewPromptFormatter formatter;

    public ReviewPromptFactory(
            @Value("classpath:prompts/review/system.st")
            Resource systemPromptResource,

            @Value(
                    "classpath:prompts/review/"
                            + "review-candidates.st"
            )
            Resource reviewPromptResource,

            TemplateRenderer templateRenderer,
            ReviewPromptFormatter formatter
    ) {
        this.systemPromptResource = systemPromptResource;
        this.reviewPromptResource = reviewPromptResource;
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

    public Message createUserMessage(ReviewRequest request) {
        Map<String, Object> variables = Map.of(
                "platform",
                request.platform(),
                "reviewLanguage",
                request.reviewLanguage(),
                "expectedReviewCount",
                request.candidates().size(),
                "strategy",
                formatter.formatStrategy(request.strategy()),
                "candidates",
                formatter.formatCandidates(
                        request.candidates()
                )
        );

        return PromptTemplate.builder()
                .resource(reviewPromptResource)
                .renderer(templateRenderer)
                .variables(variables)
                .build()
                .createMessage();
    }
}