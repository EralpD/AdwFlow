package com.example.demo.agent.review.decision;

// Results of Decision

import com.example.demo.agent.copywriter.AdCandidate;
import com.example.demo.agent.copywriter.RevisionInstruction;

import java.util.List;
import java.util.Objects;

public record ReviewDecisionResult(
        ReviewDecision decision,
        List<AdCandidate> approvedCandidates,
        List<AdCandidate> candidatesToRevise,
        List<RevisionInstruction> revisionInstructions
) {

    public ReviewDecisionResult {
        decision = Objects.requireNonNull(
                decision,
                "decision must not be null"
        );

        approvedCandidates =
                immutableList(approvedCandidates);

        candidatesToRevise =
                immutableList(candidatesToRevise);

        revisionInstructions =
                immutableList(revisionInstructions);
    }

    private static <T> List<T> immutableList(List<T> values) {
        return values == null
                ? List.of()
                : List.copyOf(values);
    }
}