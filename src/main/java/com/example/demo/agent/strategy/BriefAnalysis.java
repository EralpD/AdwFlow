package com.example.demo.agent.strategy;

import java.util.List;

// The result of the brief analysis.

public record BriefAnalysis(
        String objective,
        String productOrOffer,
        String targetAudience,
        String customerProblem,
        String keyValueProposition,
        String desiredAction,
        List<String> missingInformation,
        List<String> assumptions
) {

    public BriefAnalysis {
        missingInformation = immutableList(missingInformation);
        assumptions = immutableList(assumptions);
    }

    private static <T> List<T> immutableList(List<T> values) {
        return values == null ? List.of() : List.copyOf(values);
    }
}