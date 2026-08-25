package com.example.demo.agent.copywriter;

// Record of Ad Candidate.

import java.util.List;

public record AdCandidate(
        String candidateId,
        String sourceAngleId,
        String headline,
        String primaryText,
        String callToAction,
        List<String> hashtags,
        List<String> claimsUsed
) {

    public AdCandidate {
        hashtags = immutableList(hashtags);
        claimsUsed = immutableList(claimsUsed);
    }

    private static <T> List<T> immutableList(List<T> values) {
        return values == null
                ? List.of()
                : List.copyOf(values);
    }
}