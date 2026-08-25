package com.example.demo.agent.copywriter;

// Interface class of 2 requests

import com.example.demo.agent.strategy.StrategyResult;

public sealed interface CopywriterRequest
        permits GenerateCopyRequest, ReviseCopyRequest {

    StrategyResult strategy();

    String platform();

    String language();
}