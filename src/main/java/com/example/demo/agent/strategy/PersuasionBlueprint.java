package com.example.demo.agent.strategy;

import java.util.List;

// The class of the manipulation of human pyshocology

public record PersuasionBlueprint(
        AwarenessStage awarenessStage,
        String primaryMotivation,
        String emotionalTension,
        List<String> objections,
        String hookStrategy,
        String valueFraming,
        String proofStrategy,
        String ctaStrategy,
        List<String> prohibitedTactics
) {

    public PersuasionBlueprint {
        objections = immutableList(objections);
        prohibitedTactics = immutableList(prohibitedTactics);
    }

    private static <T> List<T> immutableList(List<T> values) {
        return values == null ? List.of() : List.copyOf(values);
    }
}