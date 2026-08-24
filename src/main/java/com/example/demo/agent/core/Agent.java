package com.example.demo.agent.core;

public interface Agent<I, O> {

    AgentDescriptor descriptor();

    O execute(I input, AgentContext context);
}