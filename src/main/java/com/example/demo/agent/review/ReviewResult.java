package com.example.demo.agent.review;

import java.util.List;

// Review Result record

public record ReviewResult(
        String reviewSummary,
        List<CandidateReview> candidateReviews
) {

    public ReviewResult {
        candidateReviews = candidateReviews == null
                ? List.of()
                : List.copyOf(candidateReviews);
    }
}