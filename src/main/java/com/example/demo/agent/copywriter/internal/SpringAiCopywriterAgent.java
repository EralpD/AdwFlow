package com.example.demo.agent.copywriter.internal;

// Internal agent file

import java.util.List;
import java.util.Objects;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.example.demo.agent.copywriter.CopywriterAgent;
import com.example.demo.agent.copywriter.CopywriterOutputValidator;
import com.example.demo.agent.copywriter.CopywriterRequest;
import com.example.demo.agent.copywriter.CopywriterResult;
import com.example.demo.agent.core.AgentContext;
import com.example.demo.agent.core.AgentDescriptor;

@Component
public final class SpringAiCopywriterAgent
        implements CopywriterAgent {

    private static final AgentDescriptor DESCRIPTOR =
            AgentDescriptor.of(
                    "copywriter",
                    "1.0.0",
                    "candidate-generation",
                    "targeted-revision",
                    "platform-aware-copy",
                    "claim-extraction"
            );

    private final ChatClient chatClient;
    private final CopywriterPromptFactory promptFactory;
    private final CopywriterOutputValidator outputValidator;

    public SpringAiCopywriterAgent(
            @Qualifier("copywriterChatClient")
            ChatClient chatClient,
            CopywriterPromptFactory promptFactory,
            CopywriterOutputValidator outputValidator
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
    public CopywriterResult execute(
            CopywriterRequest input,
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

        CopywriterResult result = chatClient
                .prompt()
                .messages(List.of(
                        systemMessage,
                        userMessage
                ))
                .call()
                .entity(CopywriterResult.class);

        return outputValidator.validate(input, result);
    }
}