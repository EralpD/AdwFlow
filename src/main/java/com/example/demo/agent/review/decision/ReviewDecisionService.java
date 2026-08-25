package com.example.demo.agent.review.decision;

/*

    If there is only WARNING:
        PASS

    If there is at least one ERROR:
        REVISION

    If there is at least one CRITICAL:
        REVISION
 */

import com.example.demo.agent.copywriter.AdCandidate;
import com.example.demo.agent.copywriter.RevisionInstruction;
import com.example.demo.agent.review.CandidateReview;
import com.example.demo.agent.review.ComplianceFinding;
import com.example.demo.agent.review.ReviewResult;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public final class ReviewDecisionService {

    private final ObservationRegistry observationRegistry;

    public ReviewDecisionService(
            ObservationRegistry observationRegistry
    ) {
        this.observationRegistry = observationRegistry;
    }

    public ReviewDecisionResult decide(
            List<AdCandidate> candidates,
            ReviewResult reviewResult
    ) {
        Observation observation = Observation.start(
                "advertising.decision",
                observationRegistry
        );

        try (Observation.Scope ignored = observation.openScope()) {
            ReviewDecisionResult result = decideReview(
                    candidates,
                    reviewResult
            );

            observation.lowCardinalityKeyValue(
                    "decision.result",
                    result.decision().name().toLowerCase()
            );

            return result;
        } catch (RuntimeException failure) {
            observation.error(failure);
            throw failure;
        } finally {
            observation.stop();
        }
    }

    private ReviewDecisionResult decideReview(
            List<AdCandidate> candidates,
            ReviewResult reviewResult
    ) {
        Map<String, CandidateReview> reviewsByCandidate =
                reviewResult.candidateReviews()
                        .stream()
                        .collect(Collectors.toMap(
                                CandidateReview::candidateId,
                                Function.identity()
                        ));

        List<AdCandidate> approvedCandidates =
                new ArrayList<>();

        List<AdCandidate> candidatesToRevise =
                new ArrayList<>();

        List<RevisionInstruction> instructions =
                new ArrayList<>();

        for (AdCandidate candidate : candidates) {
            CandidateReview review =
                    reviewsByCandidate.get(
                            candidate.candidateId()
                    );

            if (review == null) {
                throw new IllegalStateException(
                        "No review found for candidate: "
                                + candidate.candidateId()
                );
            }

            List<ComplianceFinding> blockingFindings =
                    review.findings()
                            .stream()
                            .filter(finding ->
                                    finding.severity()
                                            .requiresRevision()
                            )
                            .toList();

            if (blockingFindings.isEmpty()) {
                approvedCandidates.add(candidate);
                continue;
            }

            candidatesToRevise.add(candidate);

            List<String> problems =
                    blockingFindings.stream()
                            .map(finding ->
                                    finding.category()
                                            + ": "
                                            + finding.explanation()
                            )
                            .distinct()
                            .toList();

            List<String> requiredChanges =
                    blockingFindings.stream()
                            .map(
                                    ComplianceFinding::requiredChange
                            )
                            .distinct()
                            .toList();

            instructions.add(
                    new RevisionInstruction(
                            candidate.candidateId(),
                            problems,
                            requiredChanges,
                            review.elementsToPreserve()
                    )
            );
        }

        ReviewDecision decision =
                candidatesToRevise.isEmpty()
                        ? ReviewDecision.PASS
                        : ReviewDecision.REVISION;

        return new ReviewDecisionResult(
                decision,
                approvedCandidates,
                candidatesToRevise,
                instructions
        );
    }
}
