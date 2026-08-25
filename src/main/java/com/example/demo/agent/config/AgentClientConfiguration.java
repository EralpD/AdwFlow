package com.example.demo.agent.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AgentClientConfiguration {

    @Bean("strategyChatClient")
    ChatClient strategyChatClient(
            ChatClient.Builder builder
    ) {
        return builder.build();
    }

    @Bean("copywriterChatClient")
    ChatClient copywriterChatClient(
            ChatClient.Builder builder
    ) {
        return builder.build();
    }

    @Bean("complianceChatClient")
    ChatClient complianceChatClient(
            ChatClient.Builder builder
    ) {
        return builder.build();
}
}