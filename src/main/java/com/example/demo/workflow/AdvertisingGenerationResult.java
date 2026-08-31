package com.example.demo.workflow;

import com.example.demo.agent.copywriter.AdCandidate;
import com.example.demo.agent.review.ReviewResult;
import com.example.demo.agent.review.decision.ReviewDecisionResult;
import com.example.demo.agent.strategy.StrategyResult;
import com.example.demo.validation.DeterministicValidationResult;
import com.example.demo.workflow.context.TrustedGenerationContext;

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
        ReviewDecisionResult decision,
        TrustedGenerationContext trustedContext,
        List<String> missingInputs,
        List<RevisionFindingDelta> revisionFindingDeltas
) {

    public AdvertisingGenerationResult {
        candidates = candidates == null
                ? List.of()
                : List.copyOf(candidates);
        missingInputs = missingInputs == null ? List.of() : List.copyOf(missingInputs);
        revisionFindingDeltas = revisionFindingDeltas == null
                ? List.of()
                : List.copyOf(revisionFindingDeltas);
    }
}
