package com.example.demo.agent.review;

// General output validator

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.example.demo.agent.copywriter.AdCandidate;

@Component
public final class ReviewOutputValidator {

    public ReviewResult validate(
            ReviewRequest request,
            ReviewResult result
    ) {
        if (result == null) {
            throw new InvalidReviewOutputException(
                    "Review result must not be null"
            );
        }

        requireText(
                result.reviewSummary(),
                "reviewSummary"
        );

        Map<String, AdCandidate> expectedCandidates =
                indexCandidates(request);

        if (result.candidateReviews().size()
                != expectedCandidates.size()) {
            throw new InvalidReviewOutputException(
                    "Expected "
                            + expectedCandidates.size()
                            + " candidate reviews but received "
                            + result.candidateReviews().size()
            );
        }

        validateCandidateReviews(
                expectedCandidates,
                result
        );

        return result;
    }

    private Map<String, AdCandidate> indexCandidates(
            ReviewRequest request
    ) {
        Map<String, AdCandidate> indexed =
                new HashMap<>();

        for (AdCandidate candidate : request.candidates()) {
            if (candidate == null) {
                throw new InvalidReviewOutputException(
                        "Review request contains a null candidate"
                );
            }

            requireText(
                    candidate.candidateId(),
                    "candidate.candidateId"
            );

            AdCandidate previous = indexed.put(
                    candidate.candidateId(),
                    candidate
            );

            if (previous != null) {
                throw new InvalidReviewOutputException(
                        "Duplicate candidate in review request: "
                                + candidate.candidateId()
                );
            }
        }

        return indexed;
    }

    private void validateCandidateReviews(
            Map<String, AdCandidate> expectedCandidates,
            ReviewResult result
    ) {
        Set<String> reviewedCandidateIds =
                new HashSet<>();

        for (CandidateReview review
                : result.candidateReviews()) {
            if (review == null) {
                throw new InvalidReviewOutputException(
                        "Candidate review must not be null"
                );
            }

            requireText(
                    review.candidateId(),
                    "candidateReview.candidateId"
            );

            requireText(
                    review.sourceAngleId(),
                    review.candidateId() + ".sourceAngleId"
            );

            if (!reviewedCandidateIds.add(
                    review.candidateId()
            )) {
                throw new InvalidReviewOutputException(
                        "Duplicate review for candidate: "
                                + review.candidateId()
                );
            }

            AdCandidate expected =
                    expectedCandidates.get(
                            review.candidateId()
                    );

            if (expected == null) {
                throw new InvalidReviewOutputException(
                        "Review references an unknown candidate: "
                                + review.candidateId()
                );
            }

            if (!expected.sourceAngleId().equals(
                    review.sourceAngleId()
            )) {
                throw new InvalidReviewOutputException(
                        "Review changed the source angle for: "
                                + review.candidateId()
                );
            }

            validateConfidence(review);
            validateFindings(expected, review);
        }

        if (!reviewedCandidateIds.equals(
                expectedCandidates.keySet()
        )) {
            throw new InvalidReviewOutputException(
                    "Every candidate must be reviewed exactly once"
            );
        }
    }

    private void validateConfidence(
            CandidateReview review
    ) {
        if (Double.isNaN(review.confidence())
                || review.confidence() < 0.0
                || review.confidence() > 1.0) {
            throw new InvalidReviewOutputException(
                    review.candidateId()
                            + ".confidence must be between 0 and 1"
            );
        }
    }

    private void validateFindings(
            AdCandidate candidate,
            CandidateReview review
    ) {
        Set<String> findingCodes = new HashSet<>();

        for (ComplianceFinding finding
                : review.findings()) {
            if (finding == null) {
                throw new InvalidReviewOutputException(
                        "Finding must not be null for "
                                + review.candidateId()
                );
            }

            requireText(
                    finding.code(),
                    review.candidateId() + ".finding.code"
            );

            if (finding.category() == null) {
                throw new InvalidReviewOutputException(
                        finding.code()
                                + ".category must not be null"
                );
            }

            if (finding.severity() == null) {
                throw new InvalidReviewOutputException(
                        finding.code()
                                + ".severity must not be null"
                );
            }

            if (finding.field() == null) {
                throw new InvalidReviewOutputException(
                        finding.code()
                                + ".field must not be null"
                );
            }

            requireText(
                    finding.evidence(),
                    finding.code() + ".evidence"
            );

            requireText(
                    finding.explanation(),
                    finding.code() + ".explanation"
            );

            requireText(
                    finding.requiredChange(),
                    finding.code() + ".requiredChange"
            );

            if (!findingCodes.add(
                    normalize(finding.code())
            )) {
                throw new InvalidReviewOutputException(
                        "Duplicate finding code: "
                                + finding.code()
                );
            }

            validateEvidence(candidate, finding);
        }
    }

    private void validateEvidence(
            AdCandidate candidate,
            ComplianceFinding finding
    ) {
        if ("MISSING".equalsIgnoreCase(
                finding.evidence().trim()
        )) {
            return;
        }

        String completeCandidateText = String.join(
                " ",
                candidate.headline(),
                candidate.primaryText(),
                candidate.callToAction(),
                String.join(" ", candidate.hashtags()),
                String.join(" ", candidate.claimsUsed())
        );

        if (!normalize(completeCandidateText).contains(
                normalize(finding.evidence())
        )) {
            throw new InvalidReviewOutputException(
                    "Finding evidence was not found in candidate "
                            + candidate.candidateId()
                            + ": "
                            + finding.evidence()
            );
        }
    }

    private void requireText(
            String value,
            String fieldName
    ) {
        if (value == null || value.isBlank()) {
            throw new InvalidReviewOutputException(
                    fieldName + " must not be blank"
            );
        }
    }

    private String normalize(String value) {
        return value
                .trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ");
    }
}