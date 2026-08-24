package com.example.demo.agent.strategy;

import java.util.List;

// The final output result of Strategy Agent

public record StrategyResult(
        String strategySummary,
        BriefAnalysis briefAnalysis,
        List<CreativeAngle> creativeAngles,
        List<String> globalConstraints
) {

    public StrategyResult {
        creativeAngles = immutableList(creativeAngles);
        globalConstraints = immutableList(globalConstraints);
    }

    private static <T> List<T> immutableList(List<T> values) {
        return values == null ? List.of() : List.copyOf(values);
    }
}