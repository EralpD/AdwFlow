package com.example.demo.agent.strategy.internal;

// Agent class folder

import com.example.demo.agent.core.AgentContext;
import com.example.demo.agent.core.AgentDescriptor;
import com.example.demo.agent.strategy.CreativeStrategistAgent;
import com.example.demo.agent.strategy.StrategyOutputValidator;
import com.example.demo.agent.strategy.StrategyRequest;
import com.example.demo.agent.strategy.StrategyResult;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Set;

@Component
public final class SpringAiCreativeStrategistAgent
        implements CreativeStrategistAgent {

private static final AgentDescriptor DESCRIPTOR =
        new AgentDescriptor(
                "brief-strategy",
                "Analyzes user briefs, creates distinct creative angles "
                        + "and produces ethical persuasion directions.",
                Set.of(
                        "brief-analysis",
                        "creative-angle-generation",
                        "audience-analysis",
                        "persuasion-planning",
                        "strategy-generation"
                )
        );

    private final ChatClient chatClient;
    private final StrategyPromptFactory promptFactory;
    private final StrategyOutputValidator outputValidator;

    public SpringAiCreativeStrategistAgent(
            @Qualifier("strategyChatClient")
            ChatClient chatClient,
            StrategyPromptFactory promptFactory,
            StrategyOutputValidator outputValidator
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
    public StrategyResult execute(
            StrategyRequest input,
            AgentContext context
    ) {
        Objects.requireNonNull(input, "input must not be null");
        Objects.requireNonNull(context, "context must not be null");

        Message systemMessage =
                promptFactory.createSystemMessage();

        Message userMessage =
                promptFactory.createUserMessage(input);

        StrategyResult result = chatClient
                .prompt()
                .messages(List.of(systemMessage, userMessage))
                .call()
                .entity(StrategyResult.class);

        return outputValidator.validate(input, result);
    }
}