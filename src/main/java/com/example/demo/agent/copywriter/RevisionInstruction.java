package com.example.demo.agent.copywriter;

import java.util.List;

// Input to copywriter itself from Compliance (The system fallback into copywriter at this point.)

public record RevisionInstruction(
        String candidateId,
        List<String> problems,
        List<String> requiredChanges,
        List<String> elementsToPreserve
) {

    public RevisionInstruction {
        candidateId = requireText(
                candidateId,
                "candidateId"
        );

        problems = requireNonEmptyList(
                problems,
                "problems"
        );

        requiredChanges = requireNonEmptyList(
                requiredChanges,
                "requiredChanges"
        );

        elementsToPreserve = immutableList(elementsToPreserve);
    }

    private static String requireText(
            String value,
            String fieldName
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + " must not be blank"
            );
        }

        return value.trim();
    }

    private static List<String> requireNonEmptyList(
            List<String> values,
            String fieldName
    ) {
        List<String> result = immutableList(values);

        if (result.isEmpty()) {
            throw new IllegalArgumentException(
                    fieldName + " must not be empty"
            );
        }

        if (result.stream().anyMatch(
                value -> value == null || value.isBlank()
        )) {
            throw new IllegalArgumentException(
                    fieldName + " must not contain blank values"
            );
        }

        return result;
    }

    private static List<String> immutableList(
            List<String> values
    ) {
        return values == null
                ? List.of()
                : List.copyOf(values);
    }
}