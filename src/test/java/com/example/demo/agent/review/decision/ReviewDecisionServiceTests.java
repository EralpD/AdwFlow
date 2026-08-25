package com.example.demo.agent.review.decision;

import com.example.demo.agent.copywriter.AdCandidate;
import com.example.demo.agent.review.CandidateReview;
import com.example.demo.agent.review.ComplianceFinding;
import com.example.demo.agent.review.FindingCategory;
import com.example.demo.agent.review.FindingSeverity;
import com.example.demo.agent.review.ReviewResult;
import com.example.demo.agent.review.ReviewedField;
import org.junit.jupiter.api.Test;
import io.micrometer.observation.ObservationRegistry;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReviewDecisionServiceTests {

    private final ReviewDecisionService service =
            new ReviewDecisionService(
                    ObservationRegistry.NOOP
            );

    @Test
    void sendsOnlyBlockingCandidatesToRevision() {
        AdCandidate clearCandidate = candidate(
                "CANDIDATE_A",
                "ANGLE_A"
        );
        AdCandidate blockedCandidate = candidate(
                "CANDIDATE_B",
                "ANGLE_B"
        );

        CandidateReview clearReview = new CandidateReview(
                "CANDIDATE_A",
                "ANGLE_A",
                List.of(),
                List.of("Clear and specific"),
                List.of(),
                0.9
        );

        CandidateReview blockedReview = new CandidateReview(
                "CANDIDATE_B",
                "ANGLE_B",
                List.of(
                        new ComplianceFinding(
                                "CANDIDATE_B-F01",
                                FindingCategory.UNSUPPORTED_CLAIM,
                                FindingSeverity.ERROR,
                                ReviewedField.PRIMARY_TEXT,
                                "doubles your productivity",
                                "The measurable claim is unsupported.",
                                "Remove the unsupported multiplier."
                        )
                ),
                List.of("Strong opening"),
                List.of("Keep the opening"),
                0.95
        );

        ReviewDecisionResult result = service.decide(
                List.of(clearCandidate, blockedCandidate),
                new ReviewResult(
                        "One candidate needs revision.",
                        List.of(clearReview, blockedReview)
                )
        );

        assertEquals(ReviewDecision.REVISION, result.decision());
        assertEquals(
                List.of(clearCandidate),
                result.approvedCandidates()
        );
        assertEquals(
                List.of(blockedCandidate),
                result.candidatesToRevise()
        );
        assertEquals(
                "CANDIDATE_B",
                result.revisionInstructions()
                        .getFirst()
                        .candidateId()
        );
    }

    @Test
    void passesWhenThereAreNoBlockingFindings() {
        AdCandidate candidate = candidate(
                "CANDIDATE_A",
                "ANGLE_A"
        );

        CandidateReview review = new CandidateReview(
                "CANDIDATE_A",
                "ANGLE_A",
                List.of(
                        new ComplianceFinding(
                                "CANDIDATE_A-F01",
                                FindingCategory.OTHER,
                                FindingSeverity.WARNING,
                                ReviewedField.OVERALL,
                                "daily priority list",
                                "The phrase could be more specific.",
                                "Optionally clarify the benefit."
                        )
                ),
                List.of(),
                List.of(),
                0.8
        );

        ReviewDecisionResult result = service.decide(
                List.of(candidate),
                new ReviewResult(
                        "No blocking findings.",
                        List.of(review)
                )
        );

        assertEquals(ReviewDecision.PASS, result.decision());
        assertTrue(result.candidatesToRevise().isEmpty());
        assertTrue(result.revisionInstructions().isEmpty());
    }

    private AdCandidate candidate(
            String candidateId,
            String angleId
    ) {
        return new AdCandidate(
                candidateId,
                angleId,
                "A clear headline",
                "Build a clearer daily priority list.",
                "Start Free Trial",
                List.of(),
                List.of()
        );
    }
}
