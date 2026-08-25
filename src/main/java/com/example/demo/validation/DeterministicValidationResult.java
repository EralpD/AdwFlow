package com.example.demo.validation;

import java.util.List;

public record DeterministicValidationResult(
        boolean allValid,
        List<CandidateValidationResult> candidateResults
) {

    public DeterministicValidationResult {
        candidateResults = candidateResults == null
                ? List.of()
                : List.copyOf(candidateResults);

        boolean calculatedAllValid =
                !candidateResults.isEmpty()
                        && candidateResults.stream()
                        .allMatch(CandidateValidationResult::valid);

        if (allValid != calculatedAllValid) {
            throw new IllegalArgumentException(
                    "allValid does not match candidate results"
            );
        }
    }
}
