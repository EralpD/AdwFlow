package com.example.demo.agent.review;

import java.util.List;

// Candidate Reviewing record

public record CandidateReview(
        String candidateId,
        String sourceAngleId,
        List<ComplianceFinding> findings,
        List<String> strengths,
        List<String> elementsToPreserve,
        double confidence
) {

    public CandidateReview {
        findings = immutableList(findings);
        strengths = immutableList(strengths);
        elementsToPreserve =
                immutableList(elementsToPreserve);
    }

    private static <T> List<T> immutableList(List<T> values) {
        return values == null
                ? List.of()
                : List.copyOf(values);
    }
}