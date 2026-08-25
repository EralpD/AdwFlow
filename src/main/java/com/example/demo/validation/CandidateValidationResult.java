package com.example.demo.validation;

import java.util.List;

public record CandidateValidationResult(
        String candidateId,
        boolean valid,
        List<CandidateValidationIssue> issues
) {

    public CandidateValidationResult {
        if (candidateId == null || candidateId.isBlank()) {
            throw new IllegalArgumentException(
                    "candidateId must not be blank"
            );
        }

        candidateId = candidateId.trim();
        issues = issues == null
                ? List.of()
                : List.copyOf(issues);

        if (valid != issues.isEmpty()) {
            throw new IllegalArgumentException(
                    "valid must be true exactly when issues is empty"
            );
        }
    }
}
