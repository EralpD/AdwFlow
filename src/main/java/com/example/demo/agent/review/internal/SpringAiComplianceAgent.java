package com.example.demo.agent.review.internal;

// Calling last LLM calls.

import com.example.demo.agent.core.AgentContext;
import com.example.demo.agent.core.AgentDescriptor;
import com.example.demo.agent.review.ComplianceAgent;
import com.example.demo.agent.review.ReviewOutputValidator;
import com.example.demo.agent.review.ReviewRequest;
import com.example.demo.agent.review.ReviewResult;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
public final class SpringAiComplianceAgent
        implements ComplianceAgent {

    private static final AgentDescriptor DESCRIPTOR =
            AgentDescriptor.of(
                    "compliance",
                    "1.0.0",
                    "claim-risk-review",
                    "ethical-persuasion-review",
                    "platform-risk-review",
                    "strategy-alignment-review",
                    "revision-guidance"
            );

    private final ChatClient chatClient;
    private final ReviewPromptFactory promptFactory;
    private final ReviewOutputValidator outputValidator;

    public SpringAiComplianceAgent(
            @Qualifier("complianceChatClient")
            ChatClient chatClient,
            ReviewPromptFactory promptFactory,
            ReviewOutputValidator outputValidator
    ) {
        this.chatClient = chatClient;
        this.promptFactory = promptFactory;
        this.outputValidator = outputValidator;
    }

    @Override
    public AgentDescriptor descriptor() {
        return DESCRIPTOR;
    }

    @Override
    public ReviewResult execute(
            ReviewRequest input,
            AgentContext context
    ) {
        Objects.requireNonNull(
                input,
                "input must not be null"
        );

        Objects.requireNonNull(
                context,
                "context must not be null"
        );

        Message systemMessage =
                promptFactory.createSystemMessage();

        Message userMessage =
                promptFactory.createUserMessage(input);

        ReviewResult result = chatClient
                .prompt()
                .messages(List.of(
                        systemMessage,
                        userMessage
                ))
                .call()
                .entity(ReviewResult.class);

        return outputValidator.validate(input, result);
    }
}