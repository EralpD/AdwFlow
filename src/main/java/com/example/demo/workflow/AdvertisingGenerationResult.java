package com.example.demo.workflow;

import com.example.demo.agent.copywriter.AdCandidate;
import com.example.demo.agent.review.ReviewResult;
import com.example.demo.agent.review.decision.ReviewDecisionResult;
import com.example.demo.agent.strategy.StrategyResult;
import com.example.demo.validation.DeterministicValidationResult;

import java.util.List;

public record AdvertisingGenerationResult(
        String workflowId,
        String generationId,
        GenerationStatus status,
        int revisionRounds,
        StrategyResult strategy,
        List<AdCandidate> candidates,
        DeterministicValidationResult deterministicValidation,
        ReviewResult review,
        ReviewDecisionResult decision
) {

    public AdvertisingGenerationResult {
        candidates = candidates == null
                ? List.of()
                : List.copyOf(candidates);
    }
}
